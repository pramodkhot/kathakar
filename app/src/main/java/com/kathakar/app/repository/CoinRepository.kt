package com.kathakar.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import com.kathakar.app.util.unlockedDocId
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinRepository @Inject constructor(private val db: FirebaseFirestore) {

    /**
     * Atomically unlock an episode using a Firestore transaction.
     * No backend / no real money in MVP.
     *
     * Transaction guarantees:
     *  - reader has enough coins (else throws)
     *  - episode not already unlocked (idempotent)
     *  - reader deducted, author credited 60%, audit logs written
     *  - all succeed or all roll back
     */
    suspend fun unlockEpisode(
        userId: String,
        authorId: String,
        episode: Episode
    ): Resource<Int> {
        if (userId == authorId) return Resource.Error("Authors cannot unlock their own episodes")

        val userRef    = db.collection(FirestoreCollections.USERS).document(userId)
        val authorRef  = db.collection(FirestoreCollections.USERS).document(authorId)
        val unlockRef  = db.collection(FirestoreCollections.UNLOCKED_EPISODES)
                           .document(unlockedDocId(userId, episode.episodeId))
        val txnRef     = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        val authTxnRef = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()

        return try {
            var newBalance = 0
            db.runTransaction { t ->
                val userSnap   = t.get(userRef)
                val unlockSnap = t.get(unlockRef)

                if (unlockSnap.exists()) {
                    newBalance = (userSnap.getLong("coinBalance") ?: 0).toInt()
                    return@runTransaction   // already unlocked — no charge
                }

                val balance      = (userSnap.getLong("coinBalance") ?: 0).toInt()
                val cost         = episode.unlockCostCoins
                val authorCredit = cost * MvpConfig.AUTHOR_REVENUE_PERCENT / 100
                if (balance < cost) error("Not enough coins. Need $cost, have $balance.")

                newBalance = balance - cost

                t.update(userRef, "coinBalance", FieldValue.increment(-cost.toLong()))
                t.update(authorRef,
                    "coinBalance",       FieldValue.increment(authorCredit.toLong()),
                    "totalCoinsEarned",  FieldValue.increment(authorCredit.toLong())
                )
                t.set(unlockRef, UnlockedEpisode(
                    userId     = userId,
                    episodeId  = episode.episodeId,
                    storyId    = episode.storyId,
                    coinsSpent = cost,
                    unlockedAt = Timestamp.now()
                ))
                t.set(txnRef, CoinTransaction(
                    txnId            = txnRef.id,
                    userId           = userId,
                    type             = CoinTxnType.EPISODE_UNLOCK,
                    coinsAmount      = -cost,
                    note             = "Unlocked: ${episode.title}",
                    relatedEpisodeId = episode.episodeId,
                    createdAt        = Timestamp.now()
                ))
                t.set(authTxnRef, CoinTransaction(
                    txnId            = authTxnRef.id,
                    userId           = authorId,
                    type             = CoinTxnType.AUTHOR_EARNING,
                    coinsAmount      = authorCredit,
                    note             = "Earned: ${episode.title} (${MvpConfig.AUTHOR_REVENUE_PERCENT}%)",
                    relatedEpisodeId = episode.episodeId,
                    createdAt        = Timestamp.now()
                ))
            }.await()
            Resource.Success(newBalance)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unlock failed")
        }
    }

    suspend fun getUnlockedIds(userId: String, storyId: String): Set<String> = try {
        db.collection(FirestoreCollections.UNLOCKED_EPISODES)
            .whereEqualTo("userId", userId)
            .whereEqualTo("storyId", storyId)
            .get().await()
            .documents.mapNotNull { it.getString("episodeId") }.toSet()
    } catch (e: Exception) { emptySet() }

    suspend fun getCoinHistory(userId: String): Resource<List<CoinTransaction>> = try {
        val snap = db.collection(FirestoreCollections.COIN_TRANSACTIONS)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50).get().await()
        Resource.Success(snap.toObjects(CoinTransaction::class.java))
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }

    suspend fun getAuthorEarnings(authorId: String): Resource<List<CoinTransaction>> = try {
        val snap = db.collection(FirestoreCollections.COIN_TRANSACTIONS)
            .whereEqualTo("userId", authorId)
            .whereEqualTo("type", CoinTxnType.AUTHOR_EARNING.name)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(100).get().await()
        Resource.Success(snap.toObjects(CoinTransaction::class.java))
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }
}
