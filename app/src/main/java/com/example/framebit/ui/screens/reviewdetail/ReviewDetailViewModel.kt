package com.example.framebit.ui.screens.reviewdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.framebit.auth.AuthManager
import com.example.framebit.data.remote.firebase.FirestoreService
import com.example.framebit.data.remote.model.FriendReview
import com.example.framebit.data.remote.model.LibraryEntry
import com.example.framebit.data.remote.model.PublicUser
import com.example.framebit.data.remote.model.ReviewComment
import com.example.framebit.data.repository.FriendReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

/**
 * ViewModel del detalle de una "entrada" del usuario sobre un media.
 *
 * El reviewId tiene formato "{userId}::{mediaType}::{mediaId}".
 *
 * Resuelve el contenido en este orden:
 *   1) Hay reseña en Firestore -> se carga normal (con likes, comentarios, etc.).
 *   2) No hay reseña pero sí entrada en library -> se construye un FriendReview
 *      "minimal" (rating=null, comment=""): la pantalla solo mostrará la
 *      cabecera con el título del media, el autor y el "Visto el X" /
 *      "En watchlist desde X".
 *   3) Tampoco hay entrada en library -> review queda null (la pantalla
 *      muestra "Reseña no disponible").
 *
 * Las acciones de like / comentar solo tienen sentido en el caso 1.
 * La pantalla las desactiva cuando isMinimal=true.
 */
@HiltViewModel
class ReviewDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FriendReviewRepository,
    private val firestoreService: FirestoreService,
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

            // Intento 1: reseña real en Firestore
            val real = repository.getReview(reviewId, currentUserId)
            if (real != null) {
                _review.value = real
                _comments.value = repository.getComments(reviewId)
                _loading.value = false
                return@launch
            }

            // Intento 2: construir un FriendReview "minimal" desde library
            val minimal = buildMinimalFromLibrary()
            _review.value = minimal
            // En modo minimal no hay subcolección de comentarios
            _comments.value = emptyList()
            _loading.value = false
        }
    }

    /**
     * Construye un FriendReview "minimal" cuando no hay doc de reseña en
     * Firestore, leyendo de library. Devuelve null si tampoco hay entrada.
     */
    private suspend fun buildMinimalFromLibrary(): FriendReview? {
        val parsed = FriendReview.parseId(reviewId) ?: return null
        val (authorUserId, mediaType, mediaId) = parsed

        val entry: LibraryEntry = firestoreService.getLibraryEntry(
            userId = authorUserId,
            mediaType = mediaType,
            mediaId = mediaId
        ) ?: return null

        // Datos del autor para mostrar nombre y foto en la cabecera
        val author: PublicUser? = firestoreService.getUser(authorUserId)

        return FriendReview(
            reviewId = reviewId,
            userId = authorUserId,
            userName = author?.name ?: "",
            userPicture = author?.pictureUrl,
            mediaId = mediaId,
            mediaType = mediaType,
            mediaTitle = entry.title,
            mediaPosterPath = entry.posterPath,
            releaseYear = entry.releaseYear,
            rating = null,            // sin reseña: sin nota -> isMinimal=true
            comment = "",             // sin texto -> isMinimal=true
            isFavorite = entry.isFavorite,
            createdAt = entry.updatedAt,
            likesCount = 0,
            commentsCount = 0,
            likedByMe = false
        )
    }

    /**
     * Si la reseña es minimal (no existe en Firestore), no se puede dar like:
     * no hay doc al que asociar la subcolección.
     */
    fun toggleLike() {
        val current = _review.value ?: return
        if (current.isMinimal) return
        viewModelScope.launch {
            runCatching { repository.toggleLike(reviewId, currentUserId) }
            // Recargamos solo la review (los comentarios no cambian con un like)
            _review.value = repository.getReview(reviewId, currentUserId) ?: current
        }
    }

    /**
     * Igual que con el like: solo permitido sobre reseñas reales.
     */
    fun addComment(text: String) {
        if (text.isBlank()) return
        val current = _review.value ?: return
        if (current.isMinimal) return
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
            _review.value = repository.getReview(reviewId, currentUserId) ?: current
        }
    }

    fun deleteComment(commentId: String) {
        val current = _review.value ?: return
        if (current.isMinimal) return
        viewModelScope.launch {
            runCatching { repository.deleteComment(reviewId, commentId) }
            _comments.value = repository.getComments(reviewId)
            _review.value = repository.getReview(reviewId, currentUserId) ?: current
        }
    }

    fun isOwnComment(comment: ReviewComment): Boolean = comment.userId == currentUserId
}