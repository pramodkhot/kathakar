package com.kathakar.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.*
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import com.kathakar.app.util.libraryDocId
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(private val db: FirebaseFirestore) {

    private val storiesCol  get() = db.collection(FirestoreCollections.STORIES)
    private val episodesCol get() = db.collection(FirestoreCollections.EPISODES)

    // Stories — filter in Firestore, sort+paginate in memory — NO composite index needed
    suspend fun getStories(
        category: String? = null,
        language: String? = null,
        lastVisible: DocumentSnapshot? = null
    ): Resource<Pair<List<Story>, DocumentSnapshot?>> {
        return try {
            var q: Query = storiesCol.whereEqualTo("status", "PUBLISHED")
            category?.let { q = q.whereEqualTo("category", it) }
            language?.let  { q = q.whereEqualTo("language", it) }
            val snap   = q.get().await()
            val sorted = snap.toObjects(Story::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 }
            val startIdx = if (lastVisible != null) {
                val idx = sorted.indexOfFirst { it.storyId == lastVisible.id }
                if (idx >= 0) idx + 1 else 0
            } else 0
            val page       = sorted.drop(startIdx).take(20)
            val nextCursor = if (page.size >= 20 && startIdx + 20 < sorted.size)
                snap.documents.find { it.id == page.lastOrNull()?.storyId } else null
            Resource.Success(page to nextCursor)
        } catch (e: Exception) { Resource.Error("Failed to load stories: " + e.localizedMessage) }
    }

    suspend fun searchStories(query: String): Resource<List<Story>> {
        return try {
            val token = query.lowercase().trim().split(" ")
                .firstOrNull { it.length >= 2 } ?: return Resource.Success(emptyList())
            val snap = storiesCol.whereEqualTo("status", "PUBLISHED")
                .whereArrayContains("searchTokens", token).get().await()
            Resource.Success(snap.toObjects(Story::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error("Search failed: " + e.localizedMessage) }
    }

    suspend fun getStory(storyId: String): Resource<Story> {
        return try {
            val s = storiesCol.document(storyId).get().await().toObject(Story::class.java)
                ?: return Resource.Error("Story not found")
            Resource.Success(s)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    // Episodes — filter+sort in memory — no composite index needed
    suspend fun getEpisodes(storyId: String): Resource<List<Episode>> {
        return try {
            val snap = episodesCol.whereEqualTo("storyId", storyId).get().await()
            val list = snap.toObjects(Episode::class.java)
                .filter { it.status == "PUBLISHED" }.sortedBy { it.chapterNumber }
            Resource.Success(list)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getAuthorEpisodes(storyId: String): Resource<List<Episode>> {
        return try {
            val snap = episodesCol.whereEqualTo("storyId", storyId).get().await()
            Resource.Success(snap.toObjects(Episode::class.java).sortedBy { it.chapterNumber })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getEpisode(episodeId: String): Resource<Episode> {
        return try {
            val ep = episodesCol.document(episodeId).get().await().toObject(Episode::class.java)
                ?: return Resource.Error("Episode not found")
            Resource.Success(ep)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    // Author stories — no composite index needed
    suspend fun getAuthorStories(authorId: String): Resource<List<Story>> {
        return try {
            val snap = storiesCol.whereEqualTo("authorId", authorId).get().await()
            Resource.Success(snap.toObjects(Story::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun saveStory(story: Story): Resource<String> {
        return try {
            val ref = if (story.storyId.isEmpty()) storiesCol.document()
                      else storiesCol.document(story.storyId)
            val tokens = generateSearchTokens(story.title, story.authorName)
            val data = HashMap<String, Any>()
            data["storyId"]       = ref.id
            data["title"]         = story.title
            data["description"]   = story.description
            data["coverUrl"]      = story.coverUrl
            data["authorId"]      = story.authorId
            data["authorName"]    = story.authorName
            data["category"]      = story.category
            data["language"]      = story.language
            data["searchTokens"]  = tokens
            data["status"]        = story.status
            data["totalEpisodes"] = story.totalEpisodes
            data["totalReads"]    = story.totalReads
            data["updatedAt"]     = Timestamp.now()
            if (story.createdAt == null) data["createdAt"] = Timestamp.now()
            else data["createdAt"] = story.createdAt
            ref.set(data).await()
            if (story.storyId.isEmpty()) {
                db.collection(FirestoreCollections.USERS).document(story.authorId)
                    .update("storiesCount", FieldValue.increment(1)).await()
            }
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error("Failed to save: " + e.localizedMessage) }
    }

    suspend fun saveEpisode(episode: Episode): Resource<String> {
        return try {
            val ref = if (episode.episodeId.isEmpty()) episodesCol.document()
                      else episodesCol.document(episode.episodeId)
            val wc = episode.content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            val isFreeChapter = (episode.chapterNumber == 1)
            val data = HashMap<String, Any>()
            data["episodeId"]       = ref.id
            data["storyId"]         = episode.storyId
            data["authorId"]        = episode.authorId
            data["chapterNumber"]   = episode.chapterNumber
            data["title"]           = episode.title
            data["content"]         = episode.content
            data["wordCount"]       = wc
            data["unlockCostCoins"] = if (isFreeChapter) 0 else episode.unlockCostCoins
            data["isFree"]          = isFreeChapter
            data["status"]          = episode.status
            if (episode.createdAt == null) data["createdAt"] = Timestamp.now()
            else data["createdAt"] = episode.createdAt
            ref.set(data).await()
            if (episode.episodeId.isEmpty() && episode.status == "PUBLISHED") {
                storiesCol.document(episode.storyId)
                    .update("totalEpisodes", FieldValue.increment(1)).await()
            }
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error("Failed to save: " + e.localizedMessage) }
    }

    suspend fun deleteEpisode(episodeId: String, storyId: String): Resource<Unit> {
        return try {
            episodesCol.document(episodeId).delete().await()
            storiesCol.document(storyId)
                .update("totalEpisodes", FieldValue.increment(-1)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error("Failed to delete: " + e.localizedMessage) }
    }

    suspend fun updateEpisode(episodeId: String, title: String, content: String): Resource<Unit> {
        return try {
            val wc = content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            val updates = HashMap<String, Any>()
            updates["title"]     = title
            updates["content"]   = content
            updates["wordCount"] = wc
            episodesCol.document(episodeId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error("Failed to update: " + e.localizedMessage) }
    }
}

// ── Poem Repository ────────────────────────────────────────────────────────────
@Singleton
class PoemRepository @Inject constructor(private val db: FirebaseFirestore) {

    private val poemsCol get() = db.collection(FirestoreCollections.POEMS)
    private val likesCol get() = db.collection(FirestoreCollections.POEM_LIKES)

    // Poems — filter in Firestore, sort+paginate in memory — NO composite index needed
    suspend fun getPoems(
        format: String? = null,
        language: String? = null,
        mood: String? = null,
        lastVisible: DocumentSnapshot? = null
    ): Resource<Pair<List<Poem>, DocumentSnapshot?>> {
        return try {
            var q: Query = poemsCol.whereEqualTo("status", "PUBLISHED")
            format?.let   { q = q.whereEqualTo("format", it) }
            language?.let { q = q.whereEqualTo("language", it) }
            mood?.let     { q = q.whereEqualTo("mood", it) }
            val snap   = q.get().await()
            val sorted = snap.toObjects(Poem::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 }
            val startIdx = if (lastVisible != null) {
                val idx = sorted.indexOfFirst { it.poemId == lastVisible.id }
                if (idx >= 0) idx + 1 else 0
            } else 0
            val page       = sorted.drop(startIdx).take(30)
            val nextCursor = if (page.size >= 30 && startIdx + 30 < sorted.size)
                snap.documents.find { it.id == page.lastOrNull()?.poemId } else null
            Resource.Success(page to nextCursor)
        } catch (e: Exception) { Resource.Error("Failed to load poems: " + e.localizedMessage) }
    }

    suspend fun searchPoems(query: String): Resource<List<Poem>> {
        return try {
            val token = query.lowercase().trim().split(" ")
                .firstOrNull { it.length >= 2 } ?: return Resource.Success(emptyList())
            val snap = poemsCol.whereEqualTo("status", "PUBLISHED")
                .whereArrayContains("searchTokens", token).get().await()
            Resource.Success(snap.toObjects(Poem::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error("Search failed: " + e.localizedMessage) }
    }

    suspend fun getPoem(poemId: String): Resource<Poem> {
        return try {
            val p = poemsCol.document(poemId).get().await().toObject(Poem::class.java)
                ?: return Resource.Error("Poem not found")
            Resource.Success(p)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getAuthorPoems(authorId: String): Resource<List<Poem>> {
        return try {
            val snap = poemsCol.whereEqualTo("authorId", authorId).get().await()
            Resource.Success(snap.toObjects(Poem::class.java)
                .sortedByDescending { it.createdAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun savePoem(poem: Poem): Resource<String> {
        return try {
            val ref = if (poem.poemId.isEmpty()) poemsCol.document()
                      else poemsCol.document(poem.poemId)
            val tokens = generateSearchTokens(poem.title, poem.authorName)
            val data = HashMap<String, Any>()
            data["poemId"]         = ref.id
            data["title"]          = poem.title
            data["content"]        = poem.content
            data["authorId"]       = poem.authorId
            data["authorName"]     = poem.authorName
            data["format"]         = poem.format
            data["language"]       = poem.language
            data["mood"]           = poem.mood
            data["searchTokens"]   = tokens
            data["likesCount"]     = poem.likesCount
            data["tipsCount"]      = poem.tipsCount
            data["totalTipsCoins"] = poem.totalTipsCoins
            data["status"]         = "PUBLISHED"
            if (poem.createdAt == null) data["createdAt"] = Timestamp.now()
            else data["createdAt"] = poem.createdAt
            ref.set(data).await()
            if (poem.poemId.isEmpty()) {
                db.collection(FirestoreCollections.USERS).document(poem.authorId)
                    .update("poemsCount", FieldValue.increment(1)).await()
            }
            Resource.Success(ref.id)
        } catch (e: Exception) { Resource.Error("Failed to save poem: " + e.localizedMessage) }
    }

    suspend fun updatePoem(poemId: String, title: String, content: String,
                           format: String, language: String, mood: String): Resource<Unit> {
        return try {
            val updates = HashMap<String, Any>()
            updates["title"]    = title
            updates["content"]  = content
            updates["format"]   = format
            updates["language"] = language
            updates["mood"]     = mood
            poemsCol.document(poemId).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun deletePoem(poemId: String, authorId: String): Resource<Unit> {
        return try {
            poemsCol.document(poemId).delete().await()
            db.collection(FirestoreCollections.USERS).document(authorId)
                .update("poemsCount", FieldValue.increment(-1)).await()
            Resource.Success(Unit)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun toggleLike(userId: String, poemId: String): Resource<Boolean> {
        val likeDocId = userId + "_" + poemId
        val likeRef   = likesCol.document(likeDocId)
        val poemRef   = poemsCol.document(poemId)
        return try {
            var isNowLiked = false
            db.runTransaction { t ->
                val likeSnap = t.get(likeRef)
                if (likeSnap.exists()) {
                    t.delete(likeRef)
                    t.update(poemRef, "likesCount", FieldValue.increment(-1))
                    isNowLiked = false
                } else {
                    t.set(likeRef, PoemLike(userId, poemId, Timestamp.now()))
                    t.update(poemRef, "likesCount", FieldValue.increment(1))
                    isNowLiked = true
                }
            }.await()
            Resource.Success(isNowLiked)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Like failed") }
    }

    suspend fun isLiked(userId: String, poemId: String): Boolean {
        return try { likesCol.document(userId + "_" + poemId).get().await().exists() }
        catch (e: Exception) { false }
    }

    suspend fun tipPoet(fromUserId: String, toUserId: String,
                        poem: Poem, coins: Int): Resource<Int> {
        val safeCoins = coins.coerceIn(MvpConfig.POEM_TIP_MIN, MvpConfig.POEM_TIP_MAX)
        val fromRef = db.collection(FirestoreCollections.USERS).document(fromUserId)
        val toRef   = db.collection(FirestoreCollections.USERS).document(toUserId)
        val poemRef = poemsCol.document(poem.poemId)
        val txnFrom = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        val txnTo   = db.collection(FirestoreCollections.COIN_TRANSACTIONS).document()
        return try {
            var newBalance = 0
            db.runTransaction { t ->
                val fromSnap = t.get(fromRef)
                val balance  = (fromSnap.getLong("coinBalance") ?: 0).toInt()
                if (balance < safeCoins) error("Need " + safeCoins + " coins to tip, have " + balance)
                newBalance = balance - safeCoins
                t.update(fromRef, "coinBalance",      FieldValue.increment(-safeCoins.toLong()))
                t.update(toRef,   "coinBalance",      FieldValue.increment(safeCoins.toLong()))
                t.update(toRef,   "totalCoinsEarned", FieldValue.increment(safeCoins.toLong()))
                t.update(poemRef, "tipsCount",        FieldValue.increment(1))
                t.update(poemRef, "totalTipsCoins",   FieldValue.increment(safeCoins.toLong()))
                val tipNote     = "Tipped " + safeCoins + " coins on: " + poem.title
                val receiveNote = "Received tip for: " + poem.title
                t.set(txnFrom, CoinTransaction(txnFrom.id, fromUserId, CoinTxnType.POEM_TIP,
                    -safeCoins, tipNote, "", poem.poemId, Timestamp.now()))
                t.set(txnTo, CoinTransaction(txnTo.id, toUserId, CoinTxnType.POEM_TIP_RECEIVED,
                    safeCoins, receiveNote, "", poem.poemId, Timestamp.now()))
            }.await()
            Resource.Success(newBalance)
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Tip failed") }
    }
}

// ── Library Repository ─────────────────────────────────────────────────────────
@Singleton
class LibraryRepository @Inject constructor(private val db: FirebaseFirestore) {
    private val libCol get() = db.collection(FirestoreCollections.LIBRARY)

    suspend fun getUserLibrary(userId: String): Resource<List<LibraryEntry>> {
        return try {
            val snap = libCol.whereEqualTo("userId", userId).get().await()
            Resource.Success(snap.toObjects(LibraryEntry::class.java)
                .sortedByDescending { it.lastReadAt?.seconds ?: 0 })
        } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Failed") }
    }

    suspend fun getEntry(userId: String, storyId: String): LibraryEntry? {
        return try { libCol.document(libraryDocId(userId, storyId)).get().await()
            .toObject(LibraryEntry::class.java) } catch (e: Exception) { null }
    }

    suspend fun upsert(entry: LibraryEntry) {
        try { libCol.document(libraryDocId(entry.userId, entry.storyId)).set(entry).await() }
        catch (_: Exception) { }
    }
}
