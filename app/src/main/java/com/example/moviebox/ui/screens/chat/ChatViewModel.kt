package com.example.moviebox.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.model.ChatMessage
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.data.repository.MessagingRepository
import com.example.moviebox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val chatId: String = savedStateHandle["chatId"] ?: ""
    val otherUserId: String = savedStateHandle["otherUserId"] ?: ""
    val myUserId: String = authManager.getCachedUserId() ?: ""

    val messages: StateFlow<List<ChatMessage>> =
        if (chatId.isBlank()) MutableStateFlow(emptyList())
        else messagingRepository.observeMessages(chatId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _otherUser = MutableStateFlow<PublicUser?>(null)
    val otherUser: StateFlow<PublicUser?> = _otherUser

    init {
        viewModelScope.launch {
            try { messagingRepository.ensureSignedIn() } catch (_: Exception) {}
            try { _otherUser.value = socialRepository.getUser(otherUserId) } catch (_: Exception) {}
            if (chatId.isNotBlank() && myUserId.isNotBlank()) {
                try { messagingRepository.markChatAsRead(chatId, myUserId) } catch (_: Exception) {}
            }
        }
    }

    fun markAsRead() {
        if (chatId.isBlank() || myUserId.isBlank()) return
        viewModelScope.launch {
            try { messagingRepository.markChatAsRead(chatId, myUserId) } catch (_: Exception) {}
        }
    }

    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || chatId.isBlank() || myUserId.isBlank() || otherUserId.isBlank()) return
        viewModelScope.launch {
            try {
                messagingRepository.sendTextMessage(chatId, myUserId, otherUserId, trimmed)
            } catch (_: Exception) { }
        }
    }
}