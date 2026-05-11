package com.example.moviebox.ui.screens.newchat

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

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _users = MutableStateFlow<List<PublicUser>>(emptyList())
    val users: StateFlow<List<PublicUser>> = _users

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _startedChat = MutableStateFlow<Pair<String, String>?>(null)
    val startedChat: StateFlow<Pair<String, String>?> = _startedChat

    init { load() }

    private fun load() {
        val myId = authManager.getCachedUserId()
        if (myId.isNullOrBlank()) {
            _loading.value = false
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                messagingRepository.ensureSignedIn()
                _users.value = messagingRepository.getMutualFollows(myId)
            } catch (_: Exception) {
                _users.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun startChatWith(otherUserId: String) {
        val myId = authManager.getCachedUserId() ?: return
        if (otherUserId.isBlank() || otherUserId == myId) return
        viewModelScope.launch {
            try {
                messagingRepository.ensureSignedIn()
                val chatId = messagingRepository.startChat(myId, otherUserId)
                _startedChat.value = chatId to otherUserId
            } catch (_: Exception) { }
        }
    }

    fun consumeStartedChat() {
        _startedChat.value = null
    }
}