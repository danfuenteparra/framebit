package com.example.framebit.ui.screens.userlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.data.remote.model.PublicUser
import com.example.framebit.data.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: String = savedStateHandle["userId"] ?: ""
    /** "followers" | "following" */
    private val listType: String = savedStateHandle["listType"] ?: "followers"

    val title: String = if (listType == "followers") "Seguidores" else "Siguiendo"

    private val _users = MutableStateFlow<List<PublicUser>>(emptyList())
    val users: StateFlow<List<PublicUser>> = _users

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    init {
        loadUsers()
    }

    private fun loadUsers() {
        if (userId.isBlank()) {
            _loading.value = false
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                _users.value = if (listType == "followers")
                    socialRepository.getFollowers(userId)
                else
                    socialRepository.getFollowing(userId)
            } catch (_: Exception) {
                _users.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
