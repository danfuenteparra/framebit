package com.example.moviebox.ui.screens.reviewdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.model.FriendReview
import com.example.moviebox.data.remote.model.ReviewComment
import com.example.moviebox.data.repository.FriendReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FriendReviewRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val reviewId: String = URLDecoder.decode(
        savedStateHandle.get<String>("reviewId").orEmpty(), "UTF-8"
    )

    private val currentUserId: String get() = authManager.getCachedUserId().orEmpty()
    private val currentUserName: String get() = authManager.getCachedName().orEmpty()
    private val currentUserPicture: String? get() = authManager.getCachedPictureUrl()

    private val _review = MutableStateFlow<FriendReview?>(null)
    val review: StateFlow<FriendReview?> = _review.asStateFlow()

    private val _comments = MutableStateFlow<List<ReviewComment>>(emptyList())
    val comments: StateFlow<List<ReviewComment>> = _comments.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            _review.value = repository.getReview(reviewId, currentUserId)
            _comments.value = repository.getComments(reviewId)
            _loading.value = false
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            runCatching { repository.toggleLike(reviewId, currentUserId) }
            _review.value = repository.getReview(reviewId, currentUserId)
        }
    }

    fun addComment(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching {
                repository.addComment(
                    reviewId = reviewId,
                    commenterUserId = currentUserId,
                    commenterName = currentUserName,
                    commenterPicture = currentUserPicture,
                    text = text.trim()
                )
            }
            _comments.value = repository.getComments(reviewId)
            _review.value = repository.getReview(reviewId, currentUserId)
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            runCatching { repository.deleteComment(reviewId, commentId) }
            _comments.value = repository.getComments(reviewId)
            _review.value = repository.getReview(reviewId, currentUserId)
        }
    }

    fun isOwnComment(comment: ReviewComment): Boolean = comment.userId == currentUserId
}