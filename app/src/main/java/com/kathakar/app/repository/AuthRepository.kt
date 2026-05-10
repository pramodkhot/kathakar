package com.kathakar.app.repository

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.kathakar.app.domain.model.CoinTransaction
import com.kathakar.app.domain.model.CoinTxnType
import com.kathakar.app.domain.model.MvpConfig
import com.kathakar.app.domain.model.User
import com.kathakar.app.domain.model.UserRole
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
                            trySend(snap.toObject(User::class.java))
                        } else {
                            val fbUser = fbAuth.currentUser
                            if (fbUser != null) {
                                trySend(User(userId = fbUser.uid,
                                    name = fbUser.displayName ?: "User",
                                    email = fbUser.email ?: "",
                                    photoUrl = fbUser.photoUrl?.toString() ?: "",
                                    coinBalance = 0))  // Coins disabled — using Facebook Ads monetization
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
            val cred = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(cred).await()
            val uid = result.user?.uid ?: return Resource.Error("Sign-in failed")
            createOrUpdate(uid, result.user?.displayName ?: "User",
                result.user?.email ?: "", result.user?.photoUrl?.toString() ?: "",
                result.additionalUserInfo?.isNewUser == true)
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) {
            Resource.Error("Google sign-in failed: " + e.localizedMessage)
        }
    }

    // New: sign in directly with ID token (from Credential Manager)
    suspend fun signInWithGoogleToken(idToken: String): Resource<User> {
        return try {
            val cred = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(cred).await()
            val uid = result.user?.uid ?: return Resource.Error("Sign-in failed")
            createOrUpdate(uid, result.user?.displayName ?: "User",
                result.user?.email ?: "", result.user?.photoUrl?.toString() ?: "",
                result.additionalUserInfo?.isNewUser == true)
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) {
            Resource.Error("Google sign-in failed: " + e.localizedMessage)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Resource<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Sign-in failed")
            createOrUpdate(uid, email.substringBefore("@"), email, "", false)
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) {
            Resource.Error("Sign-in failed: " + e.localizedMessage)
        }
    }

    suspend fun register(name: String, email: String, password: String): Resource<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Resource.Error("Registration failed")
            createOrUpdate(uid, name, email, "", true)
            Resource.Success(fetchUser(uid))
        } catch (e: Exception) {
            Resource.Error("Registration failed: " + e.localizedMessage)
        }
    }

    fun signOut() = auth.signOut()

    private suspend fun createOrUpdate(uid: String, name: String, email: String, photoUrl: String, isNew: Boolean) {
        val ref  = db.collection(FirestoreCollections.USERS).document(uid)
        val snap = ref.get().await()
        if (!snap.exists() || isNew) {
            createFreshUser(uid, name, email, photoUrl)
        } else {
            val updates = HashMap<String, Any>()
            updates["name"]  = name
            updates["email"] = email
            // CRITICAL: Only update photoUrl from auth provider if:
            // 1. The user has NO custom uploaded photo (empty in Firestore), AND
            // 2. The auth provider gave us a real photo (Google avatar)
            // Never overwrite a user's uploaded profile photo with "" on email sign-in
            val existingPhoto = snap.getString("photoUrl") ?: ""
            if (existingPhoto.isEmpty() && photoUrl.isNotEmpty()) {
                updates["photoUrl"] = photoUrl
            }
            // If existingPhoto is not empty, user has a custom uploaded photo — keep it
            ref.update(updates).await()
        }
    }

    private suspend fun createFreshUser(uid: String, name: String, email: String, photoUrl: String,
                                        preferredLanguages: List<String> = emptyList()) {
        val userRef = db.collection(FirestoreCollections.USERS).document(uid)
        val txnRef  = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        val note    = "Welcome to KathaKar!"  // No signup bonus — Facebook Ads monetization
        db.batch().apply {
            set(userRef, User(userId = uid, name = name, email = email, photoUrl = photoUrl,
                bio = "", role = UserRole.WRITER, coinBalance = 0,  // No signup coins
                totalCoinsEarned = 0, followersCount = 0, followingCount = 0,
                storiesCount = 0, isBanned = false, createdAt = Timestamp.now(),
                preferredLanguages = preferredLanguages))
            set(txnRef, CoinTransaction(txnId = txnRef.id, userId = uid,
                type = CoinTxnType.SIGNUP_BONUS, coinsAmount = 0,  // No signup bonus
                note = note, createdAt = Timestamp.now()))
        }.commit().await()
    }

    // Save preferred content languages to Firestore
    // Public version of fetchUser — called to refresh UI after profile edits
    suspend fun fetchUserPublic(uid: String): User? {
        return try {
            db.collection(FirestoreCollections.USERS).document(uid)
                .get().await().toObject(User::class.java)
        } catch (_: Exception) { null }
    }

    suspend fun savePreferredLanguages(uid: String, languages: List<String>): Resource<Unit> {
        return try {
            db.collection(FirestoreCollections.USERS).document(uid)
                .update("preferredLanguages", languages).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error("Failed to save preferences: " + e.localizedMessage)
        }
    }

    private suspend fun fetchUser(uid: String): User {
        return try {
            db.collection(FirestoreCollections.USERS).document(uid)
                .get().await().toObject(User::class.java) ?: User(userId = uid)
        } catch (e: Exception) { User(userId = uid) }
    }

    fun mapToUser(docId: String, data: Map<String, Any>?): User? {
        if (data == null) return null
        return try {
            User(
                userId           = (data["userId"] as? String) ?: docId,
                name             = (data["name"] as? String) ?: "",
                email            = (data["email"] as? String) ?: "",
                photoUrl         = (data["photoUrl"] as? String) ?: "",
                bio              = (data["bio"] as? String) ?: "",
                role             = runCatching { UserRole.valueOf((data["role"] as? String) ?: "READER") }.getOrDefault(UserRole.READER),
                coinBalance      = ((data["coinBalance"] as? Long) ?: 0L).toInt(),
                totalCoinsEarned = ((data["totalCoinsEarned"] as? Long) ?: 0L).toInt(),
                followersCount   = ((data["followersCount"] as? Long) ?: 0L).toInt(),
                followingCount   = ((data["followingCount"] as? Long) ?: 0L).toInt(),
                storiesCount     = ((data["storiesCount"] as? Long) ?: 0L).toInt(),
                isBanned         = (data["isBanned"] as? Boolean) ?: false,
                createdAt        = data["createdAt"] as? Timestamp,
                preferredLanguages = (data["preferredLanguages"] as? List<*>)
                    ?.filterIsInstance<String>() ?: emptyList()
            )
        } catch (e: Exception) { null }
    }
}
