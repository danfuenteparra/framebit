package com.example.moviebox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val mediaId: Int,            // ID de la película o serie
    val mediaType: String,       // "movie" o "tv"
    val mediaTitle: String = "", // Título para mostrar en lista de reseñas
    val rating: Float,
    val comment: String,
    val createdAt: Long = System.currentTimeMillis()
)