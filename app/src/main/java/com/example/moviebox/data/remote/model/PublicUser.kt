package com.example.moviebox.data.remote.model

/**
 * Representa a un usuario tal como se guarda en Firestore.
 * El userId es el 'sub' de Auth0.
 */
data class PublicUser(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val pictureUrl: String? = null,
    val searchableName: String = "", // name.lowercase() para búsquedas
    val followersCount: Int = 0,
    val followingCount: Int = 0
) {
    // Firestore requiere constructor sin argumentos
    constructor() : this("", "", "", null, "", 0, 0)
}

/**
 * Representa una peli/serie/juego visto por un usuario.
 * Se usa para las filas "De tus amigos" en las pantallas.
 */
data class FriendActivity(
    val mediaId: Int = 0,
    val title: String = "",
    val posterPath: String? = null,
    val watchedAt: Long = 0L,
    val friendUserId: String = "",
    val friendName: String = "",
    val friendPicture: String? = null
) {
    constructor() : this(0, "", null, 0L, "", "", null)
}
