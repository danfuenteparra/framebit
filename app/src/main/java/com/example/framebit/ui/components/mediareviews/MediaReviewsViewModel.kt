package com.example.framebit.ui.components.mediareviews

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.auth.AuthManager
import com.example.framebit.data.remote.model.FriendReview
import com.example.framebit.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la sección "Reseñas de amigos" + "Todas las reseñas"
 * dentro de los detail screens (Movie, TvShow, Game).
 */
@HiltViewModel
class MediaReviewsViewModel @Inject constructor(
    private val social: SocialRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _friends = MutableStateFlow<List<FriendReview>>(emptyList())
    val friends: StateFlow<List<FriendReview>> = _friends.asStateFlow()

    private val _all = MutableStateFlow<List<FriendReview>>(emptyList())
    val all: StateFlow<List<FriendReview>> = _all.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load(mediaType: String, mediaId: Int) {
        val userId = authManager.getCachedUserId().orEmpty()
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                _friends.value = social.getFriendsReviewsForMedia(userId, mediaType, mediaId)
            }
            runCatching {
                _all.value = social.getAllReviewsForMedia(mediaType, mediaId, userId)
            }
            _loading.value = false
        }
    }
}