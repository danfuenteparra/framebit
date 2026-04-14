package com.example.moviebox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val backgroundImage: String?,
    val released: String?,
    val rating: Double,
    val ratingsCount: Int,
    val metacritic: Int?,
    val genres: String?, // Géneros separados por coma
    val platforms: String?, // Plataformas separadas por coma
    val isWatchlisted: Boolean = false,
    val isFavorite: Boolean = false,
    val isPlayed: Boolean = false,
    val addedDate: Long = System.currentTimeMillis()
)