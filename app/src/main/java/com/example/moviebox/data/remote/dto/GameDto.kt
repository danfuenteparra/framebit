package com.example.moviebox.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * DTO para videojuego básico de la API de RAWG
 * Se usa en listas de juegos (populares, top rated, etc.)
 */
data class GameDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("background_image")
    val backgroundImage: String?,

    @SerializedName("released")
    val released: String?,

    @SerializedName("rating")
    val rating: Double,

    @SerializedName("ratings_count")
    val ratingsCount: Int,

    @SerializedName("metacritic")
    val metacritic: Int?,

    @SerializedName("genres")
    val genres: List<RawgGenreDto>?,

    @SerializedName("platforms")
    val platforms: List<PlatformWrapperDto>?,

    @SerializedName("short_screenshots")
    val shortScreenshots: List<ScreenshotDto>?
)

/**
 * Respuesta paginada de la API de RAWG
 */
data class GameResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("next")
    val next: String?,

    @SerializedName("previous")
    val previous: String?,

    @SerializedName("results")
    val results: List<GameDto>
)

/**
 * DTO detallado de videojuego con información adicional
 */
data class GameDetailDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("name_original")
    val nameOriginal: String?,

    @SerializedName("description_raw")
    val descriptionRaw: String?,

    @SerializedName("background_image")
    val backgroundImage: String?,

    @SerializedName("background_image_additional")
    val backgroundImageAdditional: String?,

    @SerializedName("released")
    val released: String?,

    @SerializedName("rating")
    val rating: Double,

    @SerializedName("ratings_count")
    val ratingsCount: Int,

    @SerializedName("metacritic")
    val metacritic: Int?,

    @SerializedName("playtime")
    val playtime: Int?,

    @SerializedName("genres")
    val genres: List<RawgGenreDto>?,

    @SerializedName("platforms")
    val platforms: List<PlatformWrapperDto>?,

    @SerializedName("developers")
    val developers: List<DeveloperDto>?,

    @SerializedName("publishers")
    val publishers: List<PublisherDto>?,

    @SerializedName("esrb_rating")
    val esrbRating: EsrbRatingDto?,

    @SerializedName("website")
    val website: String?,

    @SerializedName("reddit_url")
    val redditUrl: String?
)

/**
 * DTO para capturas de pantalla de un juego
 */
data class GameScreenshotsResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("results")
    val results: List<ScreenshotDto>
)

/**
 * DTO para screenshot individual
 */
data class ScreenshotDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("image")
    val image: String
)

/**
 * Género de RAWG
 */
data class RawgGenreDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

/**
 * Respuesta de lista de géneros RAWG
 */
data class RawgGenreListResponse(
    @SerializedName("count")
    val count: Int,

    @SerializedName("results")
    val results: List<RawgGenreDto>
)

/**
 * Wrapper de plataforma (RAWG anida la plataforma en un objeto)
 */
data class PlatformWrapperDto(
    @SerializedName("platform")
    val platform: PlatformDto
)

data class PlatformDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("slug")
    val slug: String
)

data class DeveloperDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

data class PublisherDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)

data class EsrbRatingDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String
)
