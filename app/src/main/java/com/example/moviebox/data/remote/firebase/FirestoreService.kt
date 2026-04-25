package com.example.moviebox.data.remote.firebase

import com.example.moviebox.data.remote.model.FriendActivity
import com.example.moviebox.data.remote.model.PublicUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Todas las operaciones contra Firestore.
 * El ID canónico del usuario es el 'sub' de Auth0 (lo pasa quien llama).
 * Firebase Auth anónimo se usa solo para pasar las reglas de seguridad.
 */
@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {

    // =================== AUTH BRIDGE ===================

    /**
     * Llamar tras hacer login con Auth0. Hace signInAnonymously si hace falta.
     */
    suspend fun ensureFirebaseSignedIn() {
        if (firebaseAuth.currentUser == null) {
            firebaseAuth.signInAnonymously().await()
        }
    }

    // =================== USUARIOS ===================

    /**
     * Crea o actualiza el documento del usuario actual en users/{userId}.
     * Se llama tras cada login para mantener nombre/foto al día.
     */
    suspend fun upsertCurrentUser(
        userId: String,
        name: String,
        email: String,
        pictureUrl: String?
    ) {
        val ref = firestore.collection("users").document(userId)
        val existing = ref.get().await()

        val data = mapOf(
            "userId" to userId,
            "name" to name,
            "email" to email,
            "pictureUrl" to pictureUrl,
            "searchableName" to name.lowercase(),
            // counts solo se inicializan la primera vez
            "followersCount" to (existing.getLong("followersCount") ?: 0L),
            "followingCount" to (existing.getLong("followingCount") ?: 0L)
        )
        ref.set(data).await()
    }

    suspend fun getUser(userId: String): PublicUser? {
        return try {
            firestore.collection("users").document(userId)
                .get().await()
                .toObject(PublicUser::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Búsqueda por prefijo de nombre (case-insensitive).
     */
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
        } catch (e: Exception) {
            emptyList()
        }
    }

    // =================== FOLLOW / UNFOLLOW ===================

    suspend fun follow(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return

        val batch = firestore.batch()
        val now = System.currentTimeMillis()

        // currentUser → following → targetUser
        val followingRef = firestore.collection("users")
            .document(currentUserId)
            .collection("following")
            .document(targetUserId)
        batch.set(followingRef, mapOf("followedAt" to now))

        // targetUser → followers → currentUser
        val followerRef = firestore.collection("users")
            .document(targetUserId)
            .collection("followers")
            .document(currentUserId)
        batch.set(followerRef, mapOf("followedAt" to now))

        // incrementar contadores
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
            .document(currentUserId)
            .collection("following")
            .document(targetUserId)
        batch.delete(followingRef)

        val followerRef = firestore.collection("users")
            .document(targetUserId)
            .collection("followers")
            .document(currentUserId)
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
                .document(currentUserId)
                .collection("following")
                .document(targetUserId)
                .get().await()
                .exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Devuelve la lista de usuarios seguidos por userId (hidratado con datos públicos).
     */
    suspend fun getFollowing(userId: String): List<PublicUser> {
        return try {
            val ids = firestore.collection("users")
                .document(userId)
                .collection("following")
                .get().await()
                .documents.map { it.id }

            if (ids.isEmpty()) emptyList()
            else fetchUsersByIds(ids)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowers(userId: String): List<PublicUser> {
        return try {
            val ids = firestore.collection("users")
                .document(userId)
                .collection("followers")
                .get().await()
                .documents.map { it.id }

            if (ids.isEmpty()) emptyList()
            else fetchUsersByIds(ids)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Devuelve solo los IDs de los seguidos (útil para queries de actividad).
     */
    suspend fun getFollowingIds(userId: String): List<String> {
        return try {
            firestore.collection("users")
                .document(userId)
                .collection("following")
                .get().await()
                .documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchUsersByIds(ids: List<String>): List<PublicUser> {
        // Firestore whereIn soporta hasta 30 ids por query
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

    // =================== ACTIVIDAD (vistos/jugados) ===================

    /**
     * Sube o quita un item visto por el usuario.
     * mediaType: "movie" | "tv" | "game"
     */
    suspend fun setWatched(
        userId: String,
        mediaType: String,
        mediaId: Int,
        title: String,
        posterPath: String?,
        isWatched: Boolean
    ) {
        val collection = collectionForType(mediaType)
        val ref = firestore.collection("users")
            .document(userId)
            .collection(collection)
            .document(mediaId.toString())

        if (isWatched) {
            ref.set(
                mapOf(
                    "mediaId" to mediaId,
                    "title" to title,
                    "posterPath" to posterPath,
                    "watchedAt" to System.currentTimeMillis()
                )
            ).await()
        } else {
            ref.delete().await()
        }
    }

    /**
     * Devuelve la actividad reciente (vistos/jugados) de los amigos.
     */
    suspend fun getFriendsActivity(
        followingIds: List<String>,
        mediaType: String,
        limitPerFriend: Int = 10
    ): List<FriendActivity> {
        if (followingIds.isEmpty()) return emptyList()
        val collection = collectionForType(mediaType)

        val result = mutableListOf<FriendActivity>()
        val users = fetchUsersByIds(followingIds).associateBy { it.userId }

        for (friendId in followingIds) {
            try {
                val docs = firestore.collection("users")
                    .document(friendId)
                    .collection(collection)
                    .orderBy("watchedAt", Query.Direction.DESCENDING)
                    .limit(limitPerFriend.toLong())
                    .get().await()

                val friend = users[friendId] ?: continue
                docs.documents.forEach { d ->
                    result += FriendActivity(
                        mediaId = (d.getLong("mediaId") ?: 0L).toInt(),
                        title = d.getString("title") ?: "",
                        posterPath = d.getString("posterPath"),
                        watchedAt = d.getLong("watchedAt") ?: 0L,
                        friendUserId = friend.userId,
                        friendName = friend.name,
                        friendPicture = friend.pictureUrl
                    )
                }
            } catch (_: Exception) { /* ignorar este amigo */ }
        }

        return result.sortedByDescending { it.watchedAt }
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

    // =================== Helpers ===================

    private fun collectionForType(mediaType: String): String = when (mediaType) {
        "movie" -> "watched_movies"
        "tv" -> "watched_tvshows"
        "game" -> "played_games"
        else -> throw IllegalArgumentException("mediaType inválido: $mediaType")
    }
}
