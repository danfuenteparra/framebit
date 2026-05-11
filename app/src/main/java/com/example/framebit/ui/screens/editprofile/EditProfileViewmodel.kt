package com.example.framebit.ui.screens.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.auth.AuthManager
import com.example.framebit.data.repository.SocialRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val authManager: AuthManager,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseStorage: FirebaseStorage
) : ViewModel() {

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio

    private val _links = MutableStateFlow<List<String>>(emptyList())
    val links: StateFlow<List<String>> = _links

    /** URL pública actual de la foto (guardada en Firestore). */
    private val _pictureUrl = MutableStateFlow("")
    val pictureUrl: StateFlow<String> = _pictureUrl

    /** Uri local pendiente de subir a Storage. */
    private val _pendingPictureUri = MutableStateFlow<Uri?>(null)
    val pendingPictureUri: StateFlow<Uri?> = _pendingPictureUri

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val userId = authManager.getCachedUserId()
                if (!userId.isNullOrBlank()) {
                    socialRepository.ensureSignedIn()
                    val u = socialRepository.getUser(userId)
                    if (u != null) {
                        _bio.value = u.bio
                        _links.value = u.links
                        _pictureUrl.value = u.pictureUrl.orEmpty()
                    } else {
                        _pictureUrl.value = authManager.getCachedPictureUrl().orEmpty()
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun updateBio(value: String) {
        _bio.value = if (value.length > 280) value.take(280) else value
    }

    fun updatePictureUrl(value: String) {
        _pictureUrl.value = value
        // Si escribe URL, descarta cualquier upload pendiente
        if (value.isNotBlank()) _pendingPictureUri.value = null
    }

    fun clearPicture() {
        _pictureUrl.value = ""
        _pendingPictureUri.value = null
    }

    fun pickPicture(uri: Uri?) {
        _pendingPictureUri.value = uri
    }

    fun updateLink(index: Int, value: String) {
        val current = _links.value.toMutableList()
        if (index in current.indices) {
            current[index] = value
            _links.value = current
        }
    }

    fun addLink() {
        if (_links.value.size >= 5) return
        _links.value = _links.value + ""
    }

    fun removeLink(index: Int) {
        val current = _links.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _links.value = current
        }
    }

    fun save() {
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            try {
                val userId = authManager.getCachedUserId()
                if (userId.isNullOrBlank()) {
                    _error.value = "No hay sesión activa"
                    return@launch
                }
                socialRepository.ensureSignedIn()

                // Decidir la URL final:
                //  1) Si hay un Uri pendiente, subir a Storage.
                //  2) Si no, usar la URL del campo de texto (normalizada).
                val pending = _pendingPictureUri.value
                val finalPicture: String = if (pending != null) {
                    try {
                        uploadProfilePicture(userId, pending)
                    } catch (e: Exception) {
                        _error.value = "No se pudo subir la foto. Activa Firebase Storage o pega una URL."
                        return@launch
                    }
                } else {
                    val raw = _pictureUrl.value.trim()
                    when {
                        raw.isBlank() -> ""
                        !raw.startsWith("http") -> "https://$raw"
                        else -> raw
                    }
                }

                val cleanLinks = _links.value.map { it.trim() }.filter { it.isNotBlank() }

                socialRepository.updateUserProfile(
                    userId = userId,
                    bio = _bio.value.trim(),
                    links = cleanLinks,
                    pictureUrl = finalPicture
                )

                _pictureUrl.value = finalPicture
                _pendingPictureUri.value = null
                _links.value = cleanLinks
                _saved.value = true
            } catch (e: Exception) {
                _error.value = e.message ?: "Error al guardar"
            } finally {
                _saving.value = false
            }
        }
    }

    private suspend fun uploadProfilePicture(userId: String, uri: Uri): String {
        if (firebaseAuth.currentUser == null) {
            firebaseAuth.signInAnonymously().await()
        }
        val safeId = userId.replace("|", "_")
        val ref = firebaseStorage.reference
            .child("profile_pictures")
            .child("$safeId.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    fun consumeError() { _error.value = null }
}