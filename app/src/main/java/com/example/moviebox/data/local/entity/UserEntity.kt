package com.example.moviebox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey
    val userId: String,
    val email: String,
    val name: String?,
    val pictureUrl: String?,
    val lastLogin: Long = System.currentTimeMillis()
)