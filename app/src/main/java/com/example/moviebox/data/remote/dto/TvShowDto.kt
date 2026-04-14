package com.example.moviebox.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para serie de TV básica de la API de TMDB
 * Se usa en listas de series (populares, top rated, etc.)
 */
data class TvShowDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String, // Las series usan "name" en lugar de "title"

    @SerializedName("overview")
    val overview: String,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("first_air_date")
    val firstAirDate: String, // Fecha del primer episodio

    @SerializedName("vote_average")
    val voteAverage: Double,

    @SerializedName("vote_count")
    val voteCount: Int
)

/**
 * Respuesta paginada de la API para listas de series
 */
data class TvShowResponse(
    @SerializedName("page")
    val page: Int,

    @SerializedName("results")
    val results: List<TvShowDto>,

    @SerializedName("total_pages")
    val totalPages: Int,

    @SerializedName("total_results")
    val totalResults: Int
)

/**
 * DTO detallado de serie con información adicional
 * Se usa cuando se consulta una serie específica
 */
data class TvShowDetailDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("overview")
    val overview: String,

    @SerializedName("poster_path")
    val posterPath: String?,

    @SerializedName("backdrop_path")
    val backdropPath: String?,

    @SerializedName("first_air_date")
    val firstAirDate: String,

    @SerializedName("vote_average")
    val voteAverage: Double,

    @SerializedName("vote_count")
    val voteCount: Int,

    @SerializedName("number_of_seasons")
    val numberOfSeasons: Int?, // Número de temporadas

    @SerializedName("number_of_episodes")
    val numberOfEpisodes: Int?, // Número total de episodios

    @SerializedName("genres")
    val genres: List<GenreDto> // Lista de géneros (reutiliza GenreDto de MovieDto)
)