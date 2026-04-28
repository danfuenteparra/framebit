package com.example.moviebox.data.repository

import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.data.remote.model.ReviewComment
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio de alto nivel para una reseña concreta (vista detalle).
 * El reviewId tiene el formato "{userId}::{mediaType}::{mediaId}".
 */
@Singleton
class FriendReviewRepository @Inject constructor(
    private val social: SocialRepository
) {

    suspend fun getReview(reviewId: String, currentUserId: String): FriendReview? {
        val (authorId, mediaType, mediaId) = FriendReview.parseId(reviewId) ?: return null
        return social.getReviewById(authorId, mediaType, mediaId, currentUserId)
    }

    suspend fun getComments(reviewId: String): List<ReviewComment> {
        val (authorId, mediaType, mediaId) = FriendReview.parseId(reviewId) ?: return emptyList()
        return social.getCommentsForReview(authorId, mediaType, mediaId)
    }

    suspend fun toggleLike(reviewId: String, currentUserId: String): Boolean {
        val (authorId, mediaType, mediaId) = FriendReview.parseId(reviewId) ?: return false
        return social.toggleLike(authorId, mediaType, mediaId, currentUserId)
    }

    suspend fun addComment(
        reviewId: String,
        commenterUserId: String,
        commenterName: String,
        commenterPicture: String?,
        text: String
    ) {
        val (authorId, mediaType, mediaId) = FriendReview.parseId(reviewId) ?: return
        social.addComment(authorId, mediaType, mediaId, commenterUserId, commenterName, commenterPicture, text)
    }

    suspend fun deleteComment(reviewId: String, commentId: String) {
        val (authorId, mediaType, mediaId) = FriendReview.parseId(reviewId) ?: return
        social.deleteComment(authorId, mediaType, mediaId, commentId)
    }
}