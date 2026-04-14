package com.example.moviebox.data.remote.dto

import com.google.gson.annotations.SerializedName

// ========== BÚSQUEDA DE PERSONAS ==========

data class PersonResponse(
    @SerializedName("page")
    val page: Int,
    @SerializedName("results")
    val results: List<PersonDto>
)

data class PersonDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("known_for_department")
    val knownForDepartment: String,
    @SerializedName("profile_path")
    val profilePath: String?,
    @SerializedName("known_for")
    val knownFor: List<KnownForDto>
)

data class KnownForDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("media_type")
    val mediaType: String,
    @SerializedName("title")
    val title: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("vote_average")
    val voteAverage: Double
)

// ========== CRÉDITOS COMBINADOS DE UNA PERSONA ==========

data class PersonCombinedCreditsResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("cast")
    val cast: List<PersonCreditDto>,
    @SerializedName("crew")
    val crew: List<PersonCreditDto>
)

data class PersonCreditDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("media_type")
    val mediaType: String,            // "movie" o "tv"
    @SerializedName("title")
    val title: String?,               // solo películas
    @SerializedName("name")
    val name: String?,                // solo series
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("vote_average")
    val voteAverage: Double,
    @SerializedName("character")
    val character: String?,           // solo en cast
    @SerializedName("job")
    val job: String?,                 // solo en crew ("Director", etc.)
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("first_air_date")
    val firstAirDate: String?,
    @SerializedName("popularity")
    val popularity: Double
)

// ========== LISTA DE GÉNEROS ==========

data class GenreListResponse(
    @SerializedName("genres")
    val genres: List<GenreDto>
)