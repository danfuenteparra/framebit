package com.example.framebit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tv_shows")
data class TvShowEntity(
    @PrimaryKey
    val id: Int, // ID único de la serie (desde TMDb API)

    val name: String, // Nombre de la serie

    val overview: String, // Sinopsis o descripción de la trama

    val posterPath: String?, // URL del póster (imagen vertical)

    val backdropPath: String?, // URL de la imagen de fondo (imagen horizontal)

    val firstAirDate: String, // Fecha de primer episodio (formato: YYYY-MM-DD)

    val voteAverage: Double, // Puntuación promedio (0.0-10.0)

    val voteCount: Int, // Cantidad total de votos recibidos

    val isWatchlisted: Boolean = false, // Marcada para ver después

    val isFavorite: Boolean = false, // Marcada como favorita

    val isWatched: Boolean = false, // Indica si el usuario ya la vio (al menos parcialmente)

    val addedDate: Long = System.currentTimeMillis() // Timestamp de cuándo se agregó a la biblioteca
)