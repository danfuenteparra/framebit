package com.example.framebit.data.repository

import com.example.framebit.data.local.dao.ReviewDao
import com.example.framebit.data.local.entity.ReviewEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val reviewDao: ReviewDao
) {

    fun getReviewsForMedia(mediaId: Int, mediaType: String): Flow<List<ReviewEntity>> {
        return reviewDao.getReviewsForMedia(mediaId, mediaType)
    }

    fun getAllReviews(): Flow<List<ReviewEntity>> {
        return reviewDao.getAllReviews()
    }

    suspend fun insertReview(review: ReviewEntity) {
        reviewDao.insertReview(review)
    }

    suspend fun updateReview(review: ReviewEntity) {
        reviewDao.updateReview(review)
    }

    suspend fun deleteReview(review: ReviewEntity) {
        reviewDao.deleteReview(review)
    }

    suspend fun deleteReviewById(reviewId: Int) {
        reviewDao.deleteReviewById(reviewId)
    }
}