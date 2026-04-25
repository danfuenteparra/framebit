package com.example.moviebox.ui.screens.usersearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moviebox.auth.AuthManager
import com.example.moviebox.data.remote.model.PublicUser
import com.example.moviebox.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserSearchViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<PublicUser>>(emptyList())
    val results: StateFlow<List<PublicUser>> = _results

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _results.value = emptyList()
            _loading.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _loading.value = true
            try {
                val myId = authManager.getCachedUserId() ?: ""
                _results.value = socialRepository.searchUsers(newQuery, myId)
            } catch (_: Exception) {
                _results.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
