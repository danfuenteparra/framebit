package com.example.moviebox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movies")
data class MovieEntity(
    @PrimaryKey
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int,
    val isWatchlisted: Boolean = false,
    val isFavorite: Boolean = false,
    val isWatched: Boolean = false,
    val addedDate: Long = System.currentTimeMillis()
)