package com.example.framebit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // ID de la reseña

    val userId: String, // ID del usuario que escribió la reseña (Auth0 sub)

    val mediaId: Int, // ID del contenido reseñado (película, serie o juego)

    val mediaType: String, // Tipo de contenido: "movie", "tv" o "game"

    val mediaTitle: String = "", // Título del contenido para mostrar en listados sin hacer JOIN

    val rating: Float, // Puntuación de 1.0 a 5.0 otorgada por el usuario

    val comment: String, // Texto de la reseña (opinión del usuario)

    val createdAt: Long = System.currentTimeMillis() // Timestamp de creación en milisegundos
)