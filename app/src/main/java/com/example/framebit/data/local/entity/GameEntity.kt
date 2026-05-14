package com.example.framebit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val id: Int, // ID único del juego (desde API externa)

    val name: String, // Nombre del juego

    val backgroundImage: String?, // URL de la imagen de fondo

    val released: String?, // Fecha de lanzamiento (formato: YYYY-MM-DD)

    val rating: Double, // Puntuación promedio del juego

    val ratingsCount: Int, // Cantidad total de votos recibidos

    val metacritic: Int?, // Puntuación Metacritic (0-100)

    val genres: String?, // Géneros separados por coma (ej: "Action,Adventure,RPG")

    val platforms: String?, // Plataformas separadas por coma (ej: "PC,PlayStation,Xbox")

    val isWatchlisted: Boolean = false, // Marcado para jugar después

    val isFavorite: Boolean = false, // Marcado como favorito

    val isPlayed: Boolean = false, // Indica si el usuario ya jugó

    val addedDate: Long = System.currentTimeMillis() // Timestamp de cuándo se agregó a la biblioteca
)