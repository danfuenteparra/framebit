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

        val data = mapOf(
            "userId" to userId,
            "name" to name,
            "email" to email,
            "pictureUrl" to pictureUrl,
            "searchableName" to name.lowercase(),
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

    // =================== RESEÑAS (fuente de "actividad de amigos") ===================

    suspend fun upsertReview(
        userId: String,
        mediaType: String,
        mediaId: Int,
        title: String,
        posterPath: String?,
        rating: Float,
        comment: String
    ) {
        val docId = "${mediaType}_$mediaId"
        firestore.collection("users")
            .document(userId)
            .collection("reviews")
            .document(docId)
            .set(
                mapOf(
                    "mediaType" to mediaType,
                    "mediaId" to mediaId,
                    "title" to title,
                    "posterPath" to posterPath,
                    "rating" to rating,
                    "comment" to comment,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
    }

    suspend fun deleteReview(userId: String, mediaType: String, mediaId: Int) {
        val docId = "${mediaType}_$mediaId"
        firestore.collection("users")
            .document(userId)
            .collection("reviews")
            .document(docId)
            .delete().await()
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
                    result += FriendActivity(
                        mediaId = (d.getLong("mediaId") ?: 0L).toInt(),
                        title = d.getString("title") ?: "",
                        posterPath = d.getString("posterPath"),
                        watchedAt = d.getLong("createdAt") ?: 0L,
                        friendUserId = friend.userId,
                        friendName = friend.name,
                        friendPicture = friend.pictureUrl
                    )
                }
            } catch (_: Exception) { }
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
}
