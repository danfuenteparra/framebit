package com.example.moviebox.data.repository

import com.example.moviebox.data.remote.firebase.FirestoreService
import com.example.moviebox.data.remote.model.FriendActivity
import com.example.moviebox.data.remote.model.PublicUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val firestore: FirestoreService
) {

    suspend fun ensureSignedIn() = firestore.ensureFirebaseSignedIn()

    suspend fun upsertCurrentUser(userId: String, name: String, email: String, pictureUrl: String?) =
        firestore.upsertCurrentUser(userId, name, email, pictureUrl)

    suspend fun getUser(userId: String): PublicUser? = firestore.getUser(userId)

    suspend fun searchUsers(query: String, excludeUserId: String): List<PublicUser> =
        firestore.searchUsers(query, excludeUserId)

    suspend fun follow(currentUserId: String, targetUserId: String) =
        firestore.follow(currentUserId, targetUserId)

    suspend fun unfollow(currentUserId: String, targetUserId: String) =
        firestore.unfollow(currentUserId, targetUserId)

    suspend fun isFollowing(currentUserId: String, targetUserId: String): Boolean =
        firestore.isFollowing(currentUserId, targetUserId)

    suspend fun getFollowing(userId: String): List<PublicUser> = firestore.getFollowing(userId)
    suspend fun getFollowers(userId: String): List<PublicUser> = firestore.getFollowers(userId)

    // Vistos por amigos = lo que han reseñado
    suspend fun getFriendsMovies(userId: String): List<FriendActivity> {
        val ids = firestore.getFollowingIds(userId)
        return firestore.getFriendsReviewedItems(ids, "movie")
    }

    suspend fun getFriendsTvShows(userId: String): List<FriendActivity> {
        val ids = firestore.getFollowingIds(userId)
        return firestore.getFriendsReviewedItems(ids, "tv")
    }

    suspend fun getFriendsGames(userId: String): List<FriendActivity> {
        val ids = firestore.getFollowingIds(userId)
        return firestore.getFriendsReviewedItems(ids, "game")
    }

    // Sincronización de reseñas
    suspend fun syncReview(
        userId: String,
        mediaType: String,
        mediaId: Int,
        title: String,
        posterPath: String?,
        rating: Float,
        comment: String
    ) = firestore.upsertReview(userId, mediaType, mediaId, title, posterPath, rating, comment)

    suspend fun deleteReviewRemote(userId: String, mediaType: String, mediaId: Int) =
        firestore.deleteReview(userId, mediaType, mediaId)

    // Top 3
    suspend fun syncTopItem(
        userId: String,
        mediaType: String,
        position: Int,
        mediaId: Int,
        title: String,
        posterPath: String?
    ) = firestore.setTopItem(userId, mediaType, position, mediaId, title, posterPath)

    suspend fun removeTopItemRemote(userId: String, mediaType: String, position: Int) =
        firestore.removeTopItem(userId, mediaType, position)

    suspend fun getTopItems(userId: String): List<Map<String, Any?>> =
        firestore.getTopItems(userId)
}
