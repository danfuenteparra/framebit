package com.example.framebit.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.auth.AuthManager
import com.example.framebit.data.remote.dto.GameDto
import com.example.framebit.data.remote.dto.MovieDto
import com.example.framebit.data.remote.dto.TvShowDto
import com.example.framebit.data.remote.model.ChatMessage
import com.example.framebit.data.remote.model.PublicUser
import com.example.framebit.data.repository.GameRepository
import com.example.framebit.data.repository.MessagingRepository
import com.example.framebit.data.repository.MovieRepository
import com.example.framebit.data.repository.SocialRepository
import com.example.framebit.data.repository.TvShowRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChatSearchType { MOVIE, TV, GAME }

sealed class ChatSearchState {
    object Idle : ChatSearchState()
    object Loading : ChatSearchState()
    data class Movies(val items: List<MovieDto>) : ChatSearchState()
    data class TvShows(val items: List<TvShowDto>) : ChatSearchState()
    data class Games(val items: List<GameDto>) : ChatSearchState()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messagingRepository: MessagingRepository,
    private val socialRepository: SocialRepository,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val gameRepository: GameRepository,
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

    // ----- Búsqueda de contenido para enviar -----

    private val _searchType = MutableStateFlow(ChatSearchType.MOVIE)
    val searchType: StateFlow<ChatSearchType> = _searchType

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchState = MutableStateFlow<ChatSearchState>(ChatSearchState.Idle)
    val searchState: StateFlow<ChatSearchState> = _searchState

    private val tmdbApiKey = "3ec3dbb22f2043fa67e0ddf84266ad61"
    private val rawgApiKey = "3391dac64bae44c1bed7a142c6008538"

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

    // ===== Búsqueda y envío de contenido =====

    fun setSearchType(type: ChatSearchType) {
        if (_searchType.value == type) return
        _searchType.value = type
        _searchQuery.value = ""
        _searchState.value = ChatSearchState.Idle
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        if (query.length < 2) {
            _searchState.value = ChatSearchState.Idle
            return
        }
        viewModelScope.launch {
            _searchState.value = ChatSearchState.Loading
            try {
                when (_searchType.value) {
                    ChatSearchType.MOVIE -> {
                        val r = movieRepository.searchMoviesFromApi(tmdbApiKey, query).getOrDefault(emptyList())
                        _searchState.value = ChatSearchState.Movies(r)
                    }
                    ChatSearchType.TV -> {
                        val r = tvShowRepository.searchTvShowsFromApi(tmdbApiKey, query).getOrDefault(emptyList())
                        _searchState.value = ChatSearchState.TvShows(r)
                    }
                    ChatSearchType.GAME -> {
                        val r = gameRepository.searchGamesFromApi(rawgApiKey, query).getOrDefault(emptyList())
                        _searchState.value = ChatSearchState.Games(r)
                    }
                }
            } catch (_: Exception) {
                _searchState.value = ChatSearchState.Idle
            }
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchState.value = ChatSearchState.Idle
    }

    fun sendMedia(mediaType: String, mediaId: Int, title: String, posterPath: String?, releaseYear: String) {
        if (chatId.isBlank() || myUserId.isBlank() || otherUserId.isBlank()) return
        viewModelScope.launch {
            try {
                messagingRepository.sendMediaMessage(
                    chatId = chatId,
                    senderId = myUserId,
                    receiverId = otherUserId,
                    mediaType = mediaType,
                    mediaId = mediaId,
                    title = title,
                    posterPath = posterPath,
                    releaseYear = releaseYear
                )
            } catch (_: Exception) { }
        }
    }
}