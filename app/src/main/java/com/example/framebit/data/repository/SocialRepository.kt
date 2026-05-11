package com.example.framebit.data.repository

import com.example.framebit.data.remote.firebase.FirestoreService
import com.example.framebit.data.remote.model.BlockRelation
import com.example.framebit.data.remote.model.FriendActivity
import com.example.framebit.data.remote.model.FriendReview
import com.example.framebit.data.remote.model.LibraryEntry
import com.example.framebit.data.remote.model.PublicUser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocialRepository @Inject constructor(
    private val firestore: FirestoreService
) {

    suspend fun ensureSignedIn() = firestore.ensureFirebaseSignedIn()

    suspend fun upsertCurrentUser(userId: String, name: String, email: String, pictureUrl: String?) =
        firestore.upsertCurrentUser(userId, name, email, pictureUrl)

    suspend fun updateUserProfile(
        userId: String,
        bio: String? = null,
        links: List<String>? = null,
        pictureUrl: String? = null
    ) = firestore.updateUserProfile(userId, bio, links, pictureUrl)

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

    // =================== BLOCKING ===================

    suspend fun blockUser(currentUserId: String, targetUserId: String) =
        firestore.blockUser(currentUserId, targetUserId)

    suspend fun unblockUser(currentUserId: String, targetUserId: String) =
        firestore.unblockUser(currentUserId, targetUserId)

    suspend fun getBlockRelation(currentUserId: String, otherUserId: String): BlockRelation =
        firestore.getBlockRelation(currentUserId, otherUserId)

    suspend fun getBlockedUsers(userId: String): List<PublicUser> =
        firestore.getBlockedUsers(userId)

    // =================== FRIENDS ACTIVITY ===================

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

    // =================== RESEÑAS ===================

    suspend fun syncReview(
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
    ) = firestore.upsertReview(
        userId, userName, userPicture, mediaType, mediaId,
        title, posterPath, releaseYear, rating, comment, isFavorite
    )

    suspend fun deleteReviewRemote(userId: String, mediaType: String, mediaId: Int) =
        firestore.deleteReview(userId, mediaType, mediaId)

    suspend fun getReviewById(authorUserId: String, mediaType: String, mediaId: Int, currentUserId: String) =
        firestore.getReviewById(authorUserId, mediaType, mediaId, currentUserId)

    suspend fun getCommentsForReview(authorUserId: String, mediaType: String, mediaId: Int) =
        firestore.getCommentsForReview(authorUserId, mediaType, mediaId)

    suspend fun toggleLike(authorUserId: String, mediaType: String, mediaId: Int, currentUserId: String) =
        firestore.toggleLike(authorUserId, mediaType, mediaId, currentUserId)

    suspend fun addComment(
        authorUserId: String, mediaType: String, mediaId: Int,
        commenterUserId: String, commenterName: String, commenterPicture: String?, text: String
    ) = firestore.addComment(
        authorUserId, mediaType, mediaId, commenterUserId, commenterName, commenterPicture, text
    )

    suspend fun deleteComment(authorUserId: String, mediaType: String, mediaId: Int, commentId: String) =
        firestore.deleteComment(authorUserId, mediaType, mediaId, commentId)

    suspend fun getAllReviewsForMedia(mediaType: String, mediaId: Int, currentUserId: String): List<FriendReview> =
        firestore.getAllReviewsForMedia(mediaType, mediaId, currentUserId)

    suspend fun getFriendsReviewsForMedia(
        currentUserId: String, mediaType: String, mediaId: Int
    ): List<FriendReview> {
        val ids = firestore.getFollowingIds(currentUserId)
        return firestore.getFriendsReviewsForMedia(ids, mediaType, mediaId, currentUserId)
    }

    // =================== TOP 3 ===================

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

    // =================== LIBRARY ===================

    suspend fun setLibraryStatus(
        userId: String,
        mediaType: String,
        mediaId: Int,
        status: String,
        isFavorite: Boolean? = null,
        title: String? = null,
        posterPath: String? = null,
        releaseYear: String? = null
    ) = firestore.setLibraryStatus(
        userId, mediaType, mediaId, status, isFavorite, title, posterPath, releaseYear
    )

    suspend fun setLibraryFavorite(
        userId: String,
        mediaType: String,
        mediaId: Int,
        isFavorite: Boolean,
        title: String? = null,
        posterPath: String? = null,
        releaseYear: String? = null
    ) = firestore.setLibraryFavorite(
        userId, mediaType, mediaId, isFavorite, title, posterPath, releaseYear
    )

    suspend fun removeLibraryEntry(userId: String, mediaType: String, mediaId: Int) =
        firestore.removeLibraryEntry(userId, mediaType, mediaId)

    suspend fun getLibraryEntry(userId: String, mediaType: String, mediaId: Int): LibraryEntry? =
        firestore.getLibraryEntry(userId, mediaType, mediaId)

    suspend fun getLibrary(userId: String): List<LibraryEntry> =
        firestore.getLibrary(userId)

    suspend fun getReviewsByUser(userId: String): List<FriendReview> =
        firestore.getReviewsByUser(userId)
}