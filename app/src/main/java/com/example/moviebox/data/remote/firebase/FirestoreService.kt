package com.example.moviebox.data.remote.firebase

import com.example.moviebox.data.remote.model.FriendActivity
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.data.remote.model.LibraryEntry
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.data.remote.model.ReviewComment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {

    // =================== AUTH BRIDGE ===================

    suspend fun ensureFirebaseSignedIn() {
        if (firebaseAuth.currentUser == null) {
            firebaseAuth.signInAnonymously().await()
        }
    }

    // =================== USUARIOS ===================

    suspend fun upsertCurrentUser(
        userId: String,
        name: String,
        email: String,
        pictureUrl: String?
    ) {
        val ref = firestore.collection("users").document(userId)
        val existing = ref.get().await()

        // Preservamos campos editables que ya tuviera el usuario (bio, links, foto custom)
        val preservedPicture = if (existing.exists() && existing.getString("pictureUrl") != null)
            existing.getString("pictureUrl")
        else pictureUrl

        val data = mapOf(
            "userId" to userId,
            "name" to name,
            "email" to email,
            "pictureUrl" to preservedPicture,
            "searchableName" to name.lowercase(),
            "followersCount" to (existing.getLong("followersCount") ?: 0L),
            "followingCount" to (existing.getLong("followingCount") ?: 0L),
            "bio" to (existing.getString("bio") ?: ""),
            "links" to (existing.get("links") as? List<*> ?: emptyList<String>())
        )
        ref.set(data).await()
    }

    /** Actualiza datos editables del perfil: bio, links y/o foto. */
    suspend fun updateUserProfile(
        userId: String,
        bio: String?,
        links: List<String>?,
        pictureUrl: String?
    ) {
        val updates = mutableMapOf<String, Any?>()
        if (bio != null) updates["bio"] = bio
        if (links != null) updates["links"] = links
        if (pictureUrl != null) updates["pictureUrl"] = pictureUrl
        if (updates.isEmpty()) return
        firestore.collection("users").document(userId)
            .set(updates, SetOptions.merge()).await()
    }

    suspend fun getUser(userId: String): PublicUser? {
        return try {
            firestore.collection("users").document(userId)
                .get().await()
                .toObject(PublicUser::class.java)
        } catch (e: Exception) { null }
    }

    suspend fun searchUsers(query: String, excludeUserId: String): List<PublicUser> {
        if (query.isBlank()) return emptyList()
        val q = query.lowercase()
        return try {
            firestore.collection("users")
                .orderBy("searchableName")
                .startAt(q)
                .endAt(q + "\uf8ff")
                .limit(20)
                .get().await()
                .toObjects(PublicUser::class.java)
                .filter { it.userId != excludeUserId }
        } catch (e: Exception) { emptyList() }
    }

    // =================== FOLLOW / UNFOLLOW ===================

    suspend fun follow(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return
        val batch = firestore.batch()
        val now = System.currentTimeMillis()

        val followingRef = firestore.collection("users")
            .document(currentUserId).collection("following").document(targetUserId)
        batch.set(followingRef, mapOf("followedAt" to now))

        val followerRef = firestore.collection("users")
            .document(targetUserId).collection("followers").document(currentUserId)
        batch.set(followerRef, mapOf("followedAt" to now))

        batch.update(
            firestore.collection("users").document(currentUserId),
            "followingCount", FieldValue.increment(1)
        )
        batch.update(
            firestore.collection("users").document(targetUserId),
            "followersCount", FieldValue.increment(1)
        )
        batch.commit().await()
    }

    suspend fun unfollow(currentUserId: String, targetUserId: String) {
        val batch = firestore.batch()
        val followingRef = firestore.collection("users")
            .document(currentUserId).collection("following").document(targetUserId)
        batch.delete(followingRef)
        val followerRef = firestore.collection("users")
            .document(targetUserId).collection("followers").document(currentUserId)
        batch.delete(followerRef)
        batch.update(
            firestore.collection("users").document(currentUserId),
            "followingCount", FieldValue.increment(-1)
        )
        batch.update(
            firestore.collection("users").document(targetUserId),
            "followersCount", FieldValue.increment(-1)
        )
        batch.commit().await()
    }

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean {
        return try {
            firestore.collection("users")
                .document(currentUserId).collection("following").document(targetUserId)
                .get().await().exists()
        } catch (e: Exception) { false }
    }

    suspend fun getFollowing(userId: String): List<PublicUser> {
        return try {
            val ids = firestore.collection("users")
                .document(userId).collection("following")
                .get().await().documents.map { it.id }
            if (ids.isEmpty()) emptyList() else fetchUsersByIds(ids)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getFollowers(userId: String): List<PublicUser> {
        return try {
            val ids = firestore.collection("users")
                .document(userId).collection("followers")
                .get().await().documents.map { it.id }
            if (ids.isEmpty()) emptyList() else fetchUsersByIds(ids)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getFollowingIds(userId: String): List<String> {
        return try {
            firestore.collection("users")
                .document(userId).collection("following")
                .get().await().documents.map { it.id }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun fetchUsersByIds(ids: List<String>): List<PublicUser> {
        val chunks = ids.chunked(30)
        val result = mutableListOf<PublicUser>()
        for (chunk in chunks) {
            val docs = firestore.collection("users")
                .whereIn("userId", chunk)
                .get().await()
            result += docs.toObjects(PublicUser::class.java)
        }
        return result
    }

    // =================== RESEÑAS ===================

    private fun reviewRef(authorUserId: String, mediaType: String, mediaId: Int) =
        firestore.collection("users")
            .document(authorUserId)
            .collection("reviews")
            .document("${mediaType}_$mediaId")

    suspend fun upsertReview(
        userId: String,
        userName: String,
        userPicture: String?,
        mediaType: String,
        mediaId: Int,
        title: String,
        posterPath: String?,
        releaseYear: String,
        rating: Float,
        comment: String,
        isFavorite: Boolean
    ) {
        val ref = reviewRef(userId, mediaType, mediaId)
        // Preservamos contadores existentes al actualizar
        val existing = ref.get().await()
        val likesCount = existing.getLong("likesCount") ?: 0L
        val commentsCount = existing.getLong("commentsCount") ?: 0L

        ref.set(
            mapOf(
                "userId" to userId,
                "userName" to userName,
                "userPicture" to userPicture,
                "mediaType" to mediaType,
                "mediaId" to mediaId,
                "title" to title,
                "posterPath" to posterPath,
                "releaseYear" to releaseYear,
                "rating" to rating,
                "comment" to comment,
                "hasComment" to comment.isNotBlank(),
                "isFavorite" to isFavorite,
                "likesCount" to likesCount,
                "commentsCount" to commentsCount,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()

        // Auto-marca como visto en la library al reseñar
        setLibraryStatus(
            userId = userId,
            mediaType = mediaType,
            mediaId = mediaId,
            status = "watched",
            isFavorite = isFavorite,
            title = title,
            posterPath = posterPath,
            releaseYear = releaseYear,
            hasReview = true
        )
    }

    suspend fun deleteReview(userId: String, mediaType: String, mediaId: Int) {
        reviewRef(userId, mediaType, mediaId).delete().await()
        // Marcamos library.hasReview = false (sigue como visto)
        val libRef = libraryRef(userId, mediaType, mediaId)
        if (libRef.get().await().exists()) {
            libRef.update("hasReview", false).await()
        }
    }

    suspend fun getFriendsReviewedItems(
        followingIds: List<String>,
        mediaType: String,
        limitPerFriend: Int = 10
    ): List<FriendActivity> {
        if (followingIds.isEmpty()) return emptyList()
        val users = fetchUsersByIds(followingIds).associateBy { it.userId }

        val result = mutableListOf<FriendActivity>()
        for (friendId in followingIds) {
            try {
                val docs = firestore.collection("users")
                    .document(friendId)
                    .collection("reviews")
                    .whereEqualTo("mediaType", mediaType)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limitPerFriend.toLong())
                    .get().await()

                val friend = users[friendId] ?: continue
                docs.documents.forEach { d ->
                    val mediaId = (d.getLong("mediaId") ?: 0L).toInt()
                    val rating = d.getDouble("rating")?.toFloat()
                    val hasComment = d.getBoolean("hasComment")
                        ?: !d.getString("comment").isNullOrBlank()
                    val isFavorite = d.getBoolean("isFavorite") ?: false

                    result += FriendActivity(
                        mediaId = mediaId,
                        mediaType = mediaType,
                        title = d.getString("title") ?: "",
                        posterPath = d.getString("posterPath"),
                        releaseYear = d.getString("releaseYear") ?: "",
                        watchedAt = d.getLong("createdAt") ?: 0L,
                        friendUserId = friend.userId,
                        friendName = friend.name,
                        friendPicture = friend.pictureUrl,
                        reviewId = FriendReview.composeId(friend.userId, mediaType, mediaId),
                        rating = rating,
                        hasComment = hasComment,
                        isFavorite = isFavorite
                    )
                }
            } catch (_: Exception) { }
        }
        return result.sortedByDescending { it.watchedAt }
    }

    // =================== INTERACCIÓN: LIKES / COMMENTS ===================

    suspend fun getReviewById(
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        currentUserId: String
    ): FriendReview? {
        return try {
            val snap = reviewRef(authorUserId, mediaType, mediaId).get().await()
            if (!snap.exists()) return null
            val likedByMe = if (currentUserId.isBlank()) false
            else reviewRef(authorUserId, mediaType, mediaId)
                .collection("likes").document(currentUserId).get().await().exists()
            mapReviewDoc(snap, authorUserId, mediaType, mediaId, likedByMe)
        } catch (e: Exception) { null }
    }

    suspend fun getCommentsForReview(
        authorUserId: String,
        mediaType: String,
        mediaId: Int
    ): List<ReviewComment> {
        return try {
            reviewRef(authorUserId, mediaType, mediaId)
                .collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()
                .documents.mapNotNull { d ->
                    d.toObject(ReviewComment::class.java)?.copy(commentId = d.id)
                }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun toggleLike(
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        currentUserId: String
    ): Boolean {
        val ref = reviewRef(authorUserId, mediaType, mediaId)
        val likeRef = ref.collection("likes").document(currentUserId)
        val exists = likeRef.get().await().exists()
        return if (exists) {
            likeRef.delete().await()
            ref.update("likesCount", FieldValue.increment(-1)).await()
            false
        } else {
            likeRef.set(mapOf("createdAt" to System.currentTimeMillis())).await()
            ref.update("likesCount", FieldValue.increment(1)).await()
            true
        }
    }

    suspend fun addComment(
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        commenterUserId: String,
        commenterName: String,
        commenterPicture: String?,
        text: String
    ) {
        val ref = reviewRef(authorUserId, mediaType, mediaId)
        val commentRef = ref.collection("comments").document()
        commentRef.set(
            mapOf(
                "commentId" to commentRef.id,
                "userId" to commenterUserId,
                "userName" to commenterName,
                "userPicture" to commenterPicture,
                "text" to text,
                "createdAt" to System.currentTimeMillis()
            )
        ).await()
        ref.update("commentsCount", FieldValue.increment(1)).await()
    }

    suspend fun deleteComment(
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        commentId: String
    ) {
        val ref = reviewRef(authorUserId, mediaType, mediaId)
        ref.collection("comments").document(commentId).delete().await()
        ref.update("commentsCount", FieldValue.increment(-1)).await()
    }

    /**
     * Reseñas de TODOS los usuarios para un media, ordenadas por likes desc.
     * Requiere índice (collectionGroup) — Firestore te dará un enlace en el primer error.
     */
    suspend fun getAllReviewsForMedia(
        mediaType: String,
        mediaId: Int,
        currentUserId: String,
        limit: Long = 50
    ): List<FriendReview> {
        return try {
            val docs = firestore.collectionGroup("reviews")
                .whereEqualTo("mediaType", mediaType)
                .whereEqualTo("mediaId", mediaId)
                .orderBy("likesCount", Query.Direction.DESCENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get().await()

            docs.documents.mapNotNull { d ->
                val authorUserId = d.getString("userId") ?: return@mapNotNull null
                val likedByMe = if (currentUserId.isBlank()) false
                else d.reference.collection("likes")
                    .document(currentUserId).get().await().exists()
                mapReviewDoc(d, authorUserId, mediaType, mediaId, likedByMe)
            }
        } catch (e: Exception) { emptyList() }
    }

    /** Reseñas SOLO de los amigos sobre un media, ordenadas por likes desc. */
    suspend fun getFriendsReviewsForMedia(
        followingIds: List<String>,
        mediaType: String,
        mediaId: Int,
        currentUserId: String
    ): List<FriendReview> {
        if (followingIds.isEmpty()) return emptyList()
        val result = mutableListOf<FriendReview>()
        for (friendId in followingIds) {
            getReviewById(friendId, mediaType, mediaId, currentUserId)?.let { result += it }
        }
        return result.sortedByDescending { it.likesCount }
    }

    private fun mapReviewDoc(
        d: DocumentSnapshot,
        authorUserId: String,
        mediaType: String,
        mediaId: Int,
        likedByMe: Boolean
    ): FriendReview {
        return FriendReview(
            reviewId = FriendReview.composeId(authorUserId, mediaType, mediaId),
            userId = authorUserId,
            userName = d.getString("userName") ?: "",
            userPicture = d.getString("userPicture"),
            mediaId = mediaId,
            mediaType = mediaType,
            mediaTitle = d.getString("title") ?: "",
            mediaPosterPath = d.getString("posterPath"),
            releaseYear = d.getString("releaseYear") ?: "",
            rating = d.getDouble("rating")?.toFloat(),
            comment = d.getString("comment") ?: "",
            isFavorite = d.getBoolean("isFavorite") ?: false,
            createdAt = d.getLong("createdAt") ?: 0L,
            likesCount = (d.getLong("likesCount") ?: 0L).toInt(),
            commentsCount = (d.getLong("commentsCount") ?: 0L).toInt(),
            likedByMe = likedByMe
        )
    }

    // =================== TOP 3 ===================

    suspend fun setTopItem(
        userId: String,
        mediaType: String,
        position: Int,
        mediaId: Int,
        title: String,
        posterPath: String?
    ) {
        val docId = "${mediaType}_$position"
        firestore.collection("users")
            .document(userId)
            .collection("top_items")
            .document(docId)
            .set(
                mapOf(
                    "mediaType" to mediaType,
                    "position" to position,
                    "mediaId" to mediaId,
                    "title" to title,
                    "posterPath" to posterPath
                )
            ).await()
    }

    suspend fun removeTopItem(userId: String, mediaType: String, position: Int) {
        val docId = "${mediaType}_$position"
        firestore.collection("users")
            .document(userId)
            .collection("top_items")
            .document(docId)
            .delete().await()
    }

    suspend fun getTopItems(userId: String): List<Map<String, Any?>> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("top_items")
                .get().await()
                .documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // =================== LIBRARY (watchlist + watched + favoritos) ===================

    private fun libraryRef(userId: String, mediaType: String, mediaId: Int) =
        firestore.collection("users")
            .document(userId)
            .collection("library")
            .document("${mediaType}_$mediaId")

    /**
     * Crea/actualiza una entrada de library con el status indicado.
     * Si ya tenía una entrada con status "watched", NO la rebaja a "watchlist".
     * Solo actualiza los campos que se pasan; el resto se hace merge.
     */
    suspend fun setLibraryStatus(
        userId: String,
        mediaType: String,
        mediaId: Int,
        status: String,                 // "watched" | "watchlist"
        isFavorite: Boolean? = null,
        title: String? = null,
        posterPath: String? = null,
        releaseYear: String? = null,
        hasReview: Boolean? = null
    ) {
        val ref = libraryRef(userId, mediaType, mediaId)
        val existing = ref.get().await()

        // No rebajar de watched a watchlist
        val finalStatus = if (existing.getString("status") == "watched" && status == "watchlist")
            "watched" else status

        val data = mutableMapOf<String, Any?>(
            "mediaType" to mediaType,
            "mediaId" to mediaId,
            "status" to finalStatus,
            "updatedAt" to System.currentTimeMillis()
        )
        if (isFavorite != null) data["isFavorite"] = isFavorite
        if (title != null) data["title"] = title
        if (posterPath != null) data["posterPath"] = posterPath
        if (releaseYear != null) data["releaseYear"] = releaseYear
        if (hasReview != null) data["hasReview"] = hasReview

        ref.set(data, SetOptions.merge()).await()
    }

    /** Cambia solo el flag de favorito sin tocar status. Crea la entrada si no existía. */
    suspend fun setLibraryFavorite(
        userId: String,
        mediaType: String,
        mediaId: Int,
        isFavorite: Boolean,
        title: String? = null,
        posterPath: String? = null,
        releaseYear: String? = null
    ) {
        val ref = libraryRef(userId, mediaType, mediaId)
        val existing = ref.get().await()
        val data = mutableMapOf<String, Any?>(
            "mediaType" to mediaType,
            "mediaId" to mediaId,
            "isFavorite" to isFavorite,
            "updatedAt" to System.currentTimeMillis()
        )
        if (!existing.exists()) {
            // Si solo es favorito y no estaba en library, lo guardamos sin status concreto:
            // pero status es obligatorio en el modelo, lo metemos como "watchlist" por defecto.
            data["status"] = "watchlist"
        }
        if (title != null) data["title"] = title
        if (posterPath != null) data["posterPath"] = posterPath
        if (releaseYear != null) data["releaseYear"] = releaseYear

        ref.set(data, SetOptions.merge()).await()
    }

    /** Borra la entrada de library completamente. */
    suspend fun removeLibraryEntry(userId: String, mediaType: String, mediaId: Int) {
        libraryRef(userId, mediaType, mediaId).delete().await()
    }

    /** Devuelve la entrada de library (o null si no existe). */
    suspend fun getLibraryEntry(userId: String, mediaType: String, mediaId: Int): LibraryEntry? {
        return try {
            val snap = libraryRef(userId, mediaType, mediaId).get().await()
            if (!snap.exists()) null else snap.toObject(LibraryEntry::class.java)
        } catch (_: Exception) { null }
    }

    /**
     * Devuelve TODAS las entradas de library de un usuario, ordenadas por updatedAt desc.
     * Filtra en el cliente por status / mediaType / favorito.
     */
    suspend fun getLibrary(userId: String): List<LibraryEntry> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("library")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get().await()
                .toObjects(LibraryEntry::class.java)
        } catch (_: Exception) { emptyList() }
    }
}