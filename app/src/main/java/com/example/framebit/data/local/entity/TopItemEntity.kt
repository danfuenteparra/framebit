package com.example.framebit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Almacena los items del Top 3 del perfil de un usuario.
 * Cada usuario puede tener máximo 3 items por tipo de contenido (1 movie, 1 tv, 1 game).
 */
@Entity(tableName = "top_items")
data class TopItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // ID único del registro

    val userId: String = "", // Identificador del usuario (Auth0 sub), preparado para sincronización online

    val mediaType: String, // Tipo de contenido: "movie", "tv" o "game"

    val position: Int, // Posición en el top: 0 (primero), 1 (segundo), 2 (tercero)

    val mediaId: Int, // ID único del contenido asociado

    val title: String, // Título del contenido para mostrar sin hacer JOIN

    val posterPath: String?, // URL del póster/imagen para visualización rápida

    val addedDate: Long = System.currentTimeMillis() // Timestamp de cuándo se agregó al top
)