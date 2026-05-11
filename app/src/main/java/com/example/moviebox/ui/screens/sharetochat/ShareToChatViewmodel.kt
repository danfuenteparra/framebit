package com.example.moviebox.ui.screens.sharetochat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.data.repository.MessagingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Datos de lo que se quiere compartir.
 * Si `reviewId` no es null, se envía como tipo "review"; si no, como "media".
 */
data class ShareTarget(
    val mediaType: String,
    val mediaId: Int,
    val title: String,
    val posterPath: String?,
    val releaseYear: String,
    val reviewId: String? = null,
    val reviewRating: Float? = null,
    val reviewAuthorName: String? = null,
    val reviewAuthorPicture: String? = null
)

sealed class ShareUiState {
    object Idle : ShareUiState()
    object Loading : ShareUiState()
    object Sending : ShareUiState()
    object Sent : ShareUiState()
    data class Error(val message: String) : ShareUiState()
}

@HiltViewModel
class ShareToChatViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _users = MutableStateFlow<List<PublicUser>>(emptyList())
    val users: StateFlow<List<PublicUser>> = _users

    private val _state = MutableStateFlow<ShareUiState>(ShareUiState.Loading)
    val state: StateFlow<ShareUiState> = _state

    fun loadCandidates() {
        val myId = authManager.getCachedUserId() ?: return
        viewModelScope.launch {
            _state.value = ShareUiState.Loading
            try {
                messagingRepository.ensureSignedIn()
                _users.value = messagingRepository.getMutualFollows(myId)
                _state.value = ShareUiState.Idle
            } catch (e: Exception) {
                _state.value = ShareUiState.Error(e.message ?: "Error al cargar usuarios")
            }
        }
    }

    fun send(target: ShareTarget, otherUserId: String, caption: String = "") {
        val myId = authManager.getCachedUserId() ?: return
        if (otherUserId.isBlank()) return
        viewModelScope.launch {
            _state.value = ShareUiState.Sending
            try {
                messagingRepository.ensureSignedIn()
                val chatId = messagingRepository.startChat(myId, otherUserId)
                if (target.reviewId != null) {
                    messagingRepository.sendReviewMessage(
                        chatId = chatId,
                        senderId = myId,
                        receiverId = otherUserId,
                        reviewId = target.reviewId,
                        mediaType = target.mediaType,
                        mediaId = target.mediaId,
                        mediaTitle = target.title,
                        mediaPosterPath = target.posterPath,
                        releaseYear = target.releaseYear,
                        rating = target.reviewRating,
                        authorName = target.reviewAuthorName ?: "",
                        authorPicture = target.reviewAuthorPicture,
                        captionText = caption
                    )
                } else {
                    messagingRepository.sendMediaMessage(
                        chatId = chatId,
                        senderId = myId,
                        receiverId = otherUserId,
                        mediaType = target.mediaType,
                        mediaId = target.mediaId,
                        title = target.title,
                        posterPath = target.posterPath,
                        releaseYear = target.releaseYear,
                        captionText = caption
                    )
                }
                _state.value = ShareUiState.Sent
            } catch (e: Exception) {
                _state.value = ShareUiState.Error(e.message ?: "Error al enviar")
            }
        }
    }

    fun reset() {
        _state.value = ShareUiState.Idle
    }
}