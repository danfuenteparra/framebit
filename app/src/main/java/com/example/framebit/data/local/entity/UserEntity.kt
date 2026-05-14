package com.example.framebit.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey
    val userId: String, // ID único del usuario (Auth0 sub)

    val email: String, // Correo electrónico del usuario

    val name: String?, // Nombre completo del usuario (puede ser nulo)

    val pictureUrl: String?, // URL de la foto de perfil (puede ser nulo)

    val lastLogin: Long = System.currentTimeMillis() // Timestamp del último inicio de sesión
)