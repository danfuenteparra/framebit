package com.example.moviebox.data.remote.dto

import com.google.gson.annotations.SerializedName

// ========== CRÉDITOS (REPARTO) ==========

data class CreditsResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("cast")
    val cast: List<CastDto>,

    @SerializedName("crew")
    val crew: List<CrewDto>
)

data class CastDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("character")
    val character: String,

    @SerializedName("profile_path")
    val profilePath: String?,

    @SerializedName("order")
    val order: Int
)

data class CrewDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("job")
    val job: String,

    @SerializedName("department")
    val department: String,

    @SerializedName("profile_path")
    val profilePath: String?
)

// ========== VÍDEOS (TRAILERS) ==========

data class VideosResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("results")
    val results: List<VideoDto>
)

data class VideoDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("key")
    val key: String,          // ID del vídeo en YouTube/Vimeo

    @SerializedName("name")
    val name: String,

    @SerializedName("site")
    val site: String,          // "YouTube", "Vimeo"

    @SerializedName("type")
    val type: String,          // "Trailer", "Teaser", "Featurette"

    @SerializedName("official")
    val official: Boolean
)