package com.kathakar.app.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    val currentUserFlow: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            val uid = fbAuth.currentUser?.uid
            if (uid == null) {
                trySend(null)
            } else {
                db.collection(FirestoreCollections.USERS).document(uid)
                    .addSnapshotListener { snap, err ->
                        if (err != null) { trySend(null); return@addSnapshotListener }
                        if (snap != null && snap.exists()) {
                            trySend(mapToUser(snap.id, snap.data))
                        } else {
                            fbAuth.currentUser?.let { u ->
                                trySend(User(userId = u.uid, name = u.displayName ?: "User",
                                    email = u.email ?: "", coinBalance = MvpConfig.FREE_COINS_ON_SIGNUP))
                            }
                        }
                    }
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Resource<User> {
        return try {
            val cred   = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(cred).await()
            val uid    = result.user?.uid ?: return Resource.Error("Sign-in failed")
            if (result.additionalUserInfo?.isNewUser == true) {
                createUser(uid, result.user?.displayName ?: "User",
                    result.user?.email ?: "", result.user?.photoUrl?.toString() ?: "")
            } else {
                ensureDocExists(uid, result.user?.displayName ?: "User",
                    result.user?.email ?: "", result.user?.photoUrl?.toString() ?: "")
            }
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) { Resource.Error("Google sign-in failed: ${e.localizedMessage}") }
    }

    suspend fun signInWithEmail(email: String, password: String): Resource<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid    = result.user?.uid ?: return Resource.Error("Sign-in failed")
            ensureDocExists(uid, email.substringBefore("@"), email, "")
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) { Resource.Error("Sign-in failed: ${e.localizedMessage}") }
    }

    suspend fun register(name: String, email: String, password: String): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid    = result.user?.uid ?: return Resource.Error("Registration failed")
            createUser(uid, name, email, "")
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) { Resource.Error("Registration failed: ${e.localizedMessage}") }
    }

    fun signOut() = auth.signOut()

    private suspend fun createUser(uid: String, name: String, email: String, photoUrl: String) {
        val userRef = db.collection(FirestoreCollections.USERS).document(uid)
        val txnRef  = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        db.batch().apply {
            set(userRef, mapOf(
                "userId" to uid, "name" to name, "email" to email, "photoUrl" to photoUrl,
                "bio" to "", "role" to UserRole.READER.name,
                "coinBalance" to MvpConfig.FREE_COINS_ON_SIGNUP, "totalCoinsEarned" to 0,
                "followersCount" to 0, "followingCount" to 0, "storiesCount" to 0,
                "isBanned" to false, "createdAt" to Timestamp.now()
            ))
            set(txnRef, CoinTransaction(txnId = txnRef.id, userId = uid,
                type = CoinTxnType.SIGNUP_BONUS, coinsAmount = MvpConfig.FREE_COINS_ON_SIGNUP,
                note = "Welcome! ${MvpConfig.FREE_COINS_ON_SIGNUP} free coins",
                createdAt = Timestamp.now()))
        }.commit().await()
    }

    private suspend fun ensureDocExists(uid: String, name: String, email: String, photoUrl: String) {
        val ref  = db.collection(FirestoreCollections.USERS).document(uid)
        val snap = ref.get().await()
        if (!snap.exists()) {
            createUser(uid, name, email, photoUrl)
            return
        }
        // Migrate old field names to new ones
        val data    = snap.data ?: return
        val updates = mutableMapOf<String, Any>()
        if (!data.containsKey("userId"))         updates["userId"]           = uid
        if (!data.containsKey("coinBalance"))    updates["coinBalance"]      = (data["coins"] as? Long)?.toInt() ?: 0
        if (!data.containsKey("followersCount")) updates["followersCount"]   = (data["followers"] as? Long)?.toInt() ?: 0
        if (!data.containsKey("followingCount")) updates["followingCount"]   = (data["following"] as? Long)?.toInt() ?: 0
        if (!data.containsKey("storiesCount"))   updates["storiesCount"]     = (data["storiesCount"] as? Long)?.toInt() ?: 0
        if (!data.containsKey("totalCoinsEarned")) updates["totalCoinsEarned"] = (data["earnings"] as? Long)?.toInt() ?: 0
        if (!data.containsKey("isBanned"))       updates["isBanned"]         = false
        if (!data.containsKey("role"))           updates["role"]             = UserRole.READER.name
        if (!data.containsKey("bio"))            updates["bio"]              = ""
        if (updates.isNotEmpty()) ref.update(updates).await()
    }

    private suspend fun fetchUser(uid: String): User {
        return try {
            val doc = db.collection(FirestoreCollections.USERS).document(uid).get().await()
            mapToUser(doc.id, doc.data) ?: User(userId = uid)
        } catch (e: Exception) { User(userId = uid) }
    }

    fun mapToUser(docId: String, data: Map<String, Any>?): User? {
        if (data == null) return null
        return try {
            User(
                userId           = (data["userId"] as? String) ?: (data["uid"] as? String) ?: docId,
                name             = (data["name"] as? String) ?: "",
                email            = (data["email"] as? String) ?: "",
                photoUrl         = (data["photoUrl"] as? String) ?: (data["avatar"] as? String) ?: "",
                bio              = (data["bio"] as? String) ?: "",
                role             = runCatching { UserRole.valueOf((data["role"] as? String) ?: "READER") }.getOrDefault(UserRole.READER),
                coinBalance      = ((data["coinBalance"] as? Long) ?: (data["coins"] as? Long) ?: 0L).toInt(),
                totalCoinsEarned = ((data["totalCoinsEarned"] as? Long) ?: (data["earnings"] as? Long) ?: 0L).toInt(),
                followersCount   = ((data["followersCount"] as? Long) ?: (data["followers"] as? Long) ?: 0L).toInt(),
                followingCount   = ((data["followingCount"] as? Long) ?: (data["following"] as? Long) ?: 0L).toInt(),
                storiesCount     = ((data["storiesCount"] as? Long) ?: 0L).toInt(),
                isBanned         = (data["isBanned"] as? Boolean) ?: false,
                createdAt        = data["createdAt"] as? Timestamp
            )
        } catch (e: Exception) { null }
    }
}
