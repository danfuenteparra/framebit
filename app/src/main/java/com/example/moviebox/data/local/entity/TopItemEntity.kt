package com.example.moviebox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Almacena los items del Top 3 del perfil.
 * userId: identificador del usuario (Auth0 sub), preparado para sincronización online
 * mediaType: "movie", "tv", "game"
 * position: 0, 1, 2 (posición en la fila)
 */
@Entity(tableName = "top_items")
data class TopItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: String = "",
    val mediaType: String,
    val position: Int,
    val mediaId: Int,
    val title: String,
    val posterPath: String?,
    val addedDate: Long = System.currentTimeMillis()
)
