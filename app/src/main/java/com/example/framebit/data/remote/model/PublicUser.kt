package com.example.framebit.data.remote.model

/**
 * Representa a un usuario tal como se guarda en Firestore.
 * El userId es el 'sub' de Auth0 prefijado ("auth0|...") o el UID de Firebase ("firebase|...").
 */
data class PublicUser(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val pictureUrl: String? = null,
    val searchableName: String = "",
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val bio: String = "",
    val links: List<String> = emptyList()
) {
    constructor() : this("", "", "", null, "", 0, 0, "", emptyList())
}

/**
 * Entrada en la librería del usuario (Firestore: users/{userId}/library/{mediaType}_{mediaId}).
 * Sustituye y unifica lo que antes vivía en Room: watchlisted/watched/favorite.
 */
data class LibraryEntry(
    val mediaType: String = "",     // "movie" | "tv" | "game"
    val mediaId: Int = 0,
    val status: String = "",        // "watched" | "watchlist"
    val isFavorite: Boolean = false,
    val title: String = "",
    val posterPath: String? = null,
    val releaseYear: String = "",
    val hasReview: Boolean = false,
    val updatedAt: Long = 0L
) {
    constructor() : this("", 0, "", false, "", null, "", false, 0L)
}

/**
 * Peli/serie/juego visto/reseñado por un amigo.
 * Incluye los datos del preview de reseña.
 */
data class FriendActivity(
    val mediaId: Int = 0,
    val mediaType: String = "",
    val title: String = "",
    val posterPath: String? = null,
    val releaseYear: String = "",
    val watchedAt: Long = 0L,
    val friendUserId: String = "",
    val friendName: String = "",
    val friendPicture: String? = null,
    // Datos de reseña (puede no haber rating si solo marcó visto/favorito)
    val reviewId: String? = null,
    val rating: Float? = null,
    val hasComment: Boolean = false,
    val isFavorite: Boolean = false
) {
    constructor() : this(0, "", "", null, "", 0L, "", "", null, null, null, false, false)
}

/**
 * Reseña completa (pantalla de detalle / lista en detalle de contenido).
 */
data class FriendReview(
    val reviewId: String = "",            // "{userId}::{mediaType}::{mediaId}"
    val userId: String = "",
    val userName: String = "",
    val userPicture: String? = null,
    val mediaId: Int = 0,
    val mediaType: String = "",
    val mediaTitle: String = "",
    val mediaPosterPath: String? = null,
    val releaseYear: String = "",
    val rating: Float? = null,
    val comment: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val likedByMe: Boolean = false
) {
    constructor() : this("", "", "", null, 0, "", "", null, "", null, "", false, 0L, 0, 0, false)

    /** Si solo marcó como visto/favorito sin escribir reseña ni puntuar */
    val isMinimal: Boolean get() = rating == null && comment.isBlank()

    companion object {
        fun composeId(userId: String, mediaType: String, mediaId: Int) =
            "$userId::${mediaType}::$mediaId"

        /** Devuelve Triple(userId, mediaType, mediaId) o null si el id no es válido. */
        fun parseId(reviewId: String): Triple<String, String, Int>? {
            val parts = reviewId.split("::")
            if (parts.size != 3) return null
            val mediaId = parts[2].toIntOrNull() ?: return null
            return Triple(parts[0], parts[1], mediaId)
        }
    }
}

/**
 * Comentario en una reseña.
 */
data class ReviewComment(
    val commentId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPicture: String? = null,
    val text: String = "",
    val createdAt: Long = 0L
) {
    constructor() : this("", "", "", null, "", 0L)
}