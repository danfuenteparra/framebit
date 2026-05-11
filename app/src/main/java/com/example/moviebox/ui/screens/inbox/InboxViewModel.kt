package com.example.moviebox.ui.screens.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.model.ChatThread
import com.example.moviebox.data.repository.MessagingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val myUserId: String = authManager.getCachedUserId() ?: ""

    val chats: StateFlow<List<ChatThread>> =
        if (myUserId.isBlank()) {
            MutableStateFlow(emptyList())
        } else {
            messagingRepository.observeChats(myUserId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    init {
        if (myUserId.isNotBlank()) {
            viewModelScope.launch {
                try { messagingRepository.ensureSignedIn() } catch (_: Exception) {}
            }
        }
    }
}