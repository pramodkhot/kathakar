package com.kathakar.app.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kathakar.app.domain.model.*
import com.kathakar.app.util.FirestoreCollections
import com.kathakar.app.util.Resource
import com.kathakar.app.util.libraryDocId
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val PAGE_SIZE = 15L

@Singleton
class StoryRepository @Inject constructor(private val db: FirebaseFirestore) {

    private val storiesCol  get() = db.collection(FirestoreCollections.STORIES)
    private val episodesCol get() = db.collection(FirestoreCollections.EPISODES)

    // ── Paginated published stories ───────────────────────────────────────────
    suspend fun getStories(
        category: String? = null,
        language: String? = null,
        lastVisible: DocumentSnapshot? = null
    ): Resource<Pair<List<Story>, DocumentSnapshot?>> = safeCall {
        var q: Query = storiesCol
            .whereEqualTo("status", StoryStatus.PUBLISHED.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
        category?.let { q = q.whereEqualTo("category", it) }
        language?.let  { q = q.whereEqualTo("language", it) }
        lastVisible?.let { q = q.startAfter(it) }
        val snap = q.get().await()
        val list = snap.toObjects(Story::class.java)
        val next = if (snap.size() >= PAGE_SIZE) snap.documents.lastOrNull() else null
        list to next
    }

    // ── Search by prefix tokens ───────────────────────────────────────────────
    suspend fun searchStories(query: String): Resource<List<Story>> = safeCall {
        val token = query.lowercase().trim().split(" ")
            .firstOrNull { it.length >= 2 } ?: return@safeCall emptyList()
        storiesCol
            .whereEqualTo("status", StoryStatus.PUBLISHED.name)
            .whereArrayContains("searchTokens", token)
            .limit(20).get().await().toObjects(Story::class.java)
    }

    // ── Single story ──────────────────────────────────────────────────────────
    suspend fun getStory(storyId: String): Resource<Story> = safeCall {
        storiesCol.document(storyId).get().await()
            .toObject(Story::class.java) ?: error("Story not found")
    }

    // ── Published episodes for a story ────────────────────────────────────────
    suspend fun getEpisodes(storyId: String): Resource<List<Episode>> = safeCall {
        episodesCol
            .whereEqualTo("storyId", storyId)
            .whereEqualTo("status", EpisodeStatus.PUBLISHED.name)
            .orderBy("chapterNumber", Query.Direction.ASCENDING)
            .get().await().toObjects(Episode::class.java)
    }

    // ── Single episode ────────────────────────────────────────────────────────
    suspend fun getEpisode(episodeId: String): Resource<Episode> = safeCall {
        episodesCol.document(episodeId).get().await()
            .toObject(Episode::class.java) ?: error("Episode not found")
    }

    // ── Author: my stories ────────────────────────────────────────────────────
    suspend fun getAuthorStories(authorId: String): Resource<List<Story>> = safeCall {
        storiesCol
            .whereEqualTo("authorId", authorId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await().toObjects(Story::class.java)
    }

    // ── Author: create / update story ─────────────────────────────────────────
    suspend fun saveStory(story: Story): Resource<String> = safeCall {
        val ref  = if (story.storyId.isEmpty()) storiesCol.document()
                   else storiesCol.document(story.storyId)
        val save = story.copy(
            storyId      = ref.id,
            searchTokens = story.generateTokens(),
            updatedAt    = Timestamp.now(),
            createdAt    = story.createdAt ?: Timestamp.now()
        )
        ref.set(save).await()
        ref.id
    }

    // ── Author: create / update episode ──────────────────────────────────────
    suspend fun saveEpisode(episode: Episode): Resource<String> = safeCall {
        val ref  = if (episode.episodeId.isEmpty()) episodesCol.document()
                   else episodesCol.document(episode.episodeId)
        val wc   = episode.content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val save = episode.copy(
            episodeId   = ref.id,
            wordCount   = wc,
            isFree      = episode.chapterNumber == 1,
            createdAt   = episode.createdAt ?: Timestamp.now(),
            publishedAt = if (episode.status == EpisodeStatus.PUBLISHED) Timestamp.now() else null
        )
        ref.set(save).await()
        // Increment story episode count on first publish
        if (episode.episodeId.isEmpty() && episode.status == EpisodeStatus.PUBLISHED) {
            storiesCol.document(episode.storyId)
                .update("totalEpisodes", FieldValue.increment(1)).await()
        }
        ref.id
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Resource<T> = try {
        Resource.Success(block())
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Error")
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Singleton
class LibraryRepository @Inject constructor(private val db: FirebaseFirestore) {

    private val libraryCol get() = db.collection(FirestoreCollections.LIBRARY)

    suspend fun getUserLibrary(userId: String): Resource<List<LibraryEntry>> = try {
        val snap = libraryCol.whereEqualTo("userId", userId)
            .orderBy("lastReadAt", Query.Direction.DESCENDING).get().await()
        Resource.Success(snap.toObjects(LibraryEntry::class.java))
    } catch (e: Exception) { Resource.Error(e.localizedMessage ?: "Error") }

    suspend fun getEntry(userId: String, storyId: String): LibraryEntry? = try {
        libraryCol.document(libraryDocId(userId, storyId)).get().await()
            .toObject(LibraryEntry::class.java)
    } catch (e: Exception) { null }

    suspend fun upsert(entry: LibraryEntry) {
        try {
            libraryCol.document(libraryDocId(entry.userId, entry.storyId)).set(entry).await()
        } catch (_: Exception) { }
    }

    suspend fun toggleBookmark(userId: String, storyId: String): Boolean = try {
        val ref     = libraryCol.document(libraryDocId(userId, storyId))
        val current = ref.get().await().getBoolean("isBookmarked") ?: false
        ref.update("isBookmarked", !current).await()
        !current
    } catch (e: Exception) { false }
}
