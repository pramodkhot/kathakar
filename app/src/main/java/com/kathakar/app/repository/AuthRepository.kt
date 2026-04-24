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
    // Live auth + user profile stream
    val currentUserFlow: Flow<User?> = callbackFlow {
        val authListener = FirebaseAuth.AuthStateListener { fbAuth ->
            val uid = fbAuth.currentUser?.uid
            if (uid == null) { trySend(null); return@AuthStateListener }
            db.collection(FirestoreCollections.USERS).document(uid)
                .addSnapshotListener { snap, _ -> trySend(snap?.toObject(User::class.java)) }
        }
        auth.addAuthStateListener(authListener)
        awaitClose { auth.removeAuthStateListener(authListener) }
    }

    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun signInWithGoogle(account: GoogleSignInAccount): Resource<User> = safeCall {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val result     = auth.signInWithCredential(credential).await()
        val uid        = result.user?.uid ?: error("No UID")
        if (result.additionalUserInfo?.isNewUser == true) {
            grantFreeCoins(uid, result.user?.displayName ?: "Reader",
                result.user?.email ?: "", result.user?.photoUrl?.toString() ?: "")
        }
        fetchUser(uid)
    }

    suspend fun signInWithEmail(email: String, password: String): Resource<User> = safeCall {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        fetchUser(result.user?.uid ?: error("No UID"))
    }

    suspend fun register(name: String, email: String, password: String): Resource<User> = safeCall {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid    = result.user?.uid ?: error("No UID")
        grantFreeCoins(uid, name, email, "")
        fetchUser(uid)
    }

    fun signOut() = auth.signOut()

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Batch write: creates user document + coin_transaction in one atomic operation.
     * Every new user starts with MvpConfig.FREE_COINS_ON_SIGNUP coins (100 by default).
     */
    private suspend fun grantFreeCoins(uid: String, name: String, email: String, photoUrl: String) {
        val userRef = db.collection(FirestoreCollections.USERS).document(uid)
        val txnRef  = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()

        db.batch().apply {
            set(userRef, User(
                userId      = uid,
                name        = name,
                email       = email,
                photoUrl    = photoUrl,
                role        = UserRole.READER,
                coinBalance = MvpConfig.FREE_COINS_ON_SIGNUP,
                createdAt   = Timestamp.now()
            ))
            set(txnRef, CoinTransaction(
                txnId       = txnRef.id,
                userId      = uid,
                type        = CoinTxnType.SIGNUP_BONUS,
                coinsAmount = MvpConfig.FREE_COINS_ON_SIGNUP,
                note        = "Welcome! ${MvpConfig.FREE_COINS_ON_SIGNUP} free coins (preview build)",
                createdAt   = Timestamp.now()
            ))
        }.commit().await()
    }

    private suspend fun fetchUser(uid: String): User =
        db.collection(FirestoreCollections.USERS).document(uid)
            .get().await().toObject(User::class.java) ?: User(userId = uid)

    private suspend fun <T> safeCall(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Something went wrong")
    }
}
