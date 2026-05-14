package com.example.framebit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    val id: Int, // ID único de la película (desde TMDb API)

    val title: String, // Título de la película

    val overview: String, // Sinopsis o descripción de la trama

    val posterPath: String?, // URL del póster (imagen vertical)

    val backdropPath: String?, // URL de la imagen de fondo (imagen horizontal)

    val releaseDate: String, // Fecha de estreno (formato: YYYY-MM-DD)

    val voteAverage: Double, // Puntuación promedio (0.0-10.0)

    val voteCount: Int, // Cantidad total de votos recibidos

    val isWatchlisted: Boolean = false, // Marcada para ver después

    val isFavorite: Boolean = false, // Marcada como favorita

    val isWatched: Boolean = false, // Indica si el usuario ya la vio

    val addedDate: Long = System.currentTimeMillis() // Timestamp de cuándo se agregó a la biblioteca
)