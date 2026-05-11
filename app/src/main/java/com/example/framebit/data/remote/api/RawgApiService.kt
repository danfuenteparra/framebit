package com.example.framebit.data.remote.api

import com.example.framebit.data.remote.dto.GameDetailDto
import com.example.framebit.data.remote.dto.GameResponse
import com.example.framebit.data.remote.dto.GameScreenshotsResponse
import com.example.framebit.data.remote.dto.RawgGenreListResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApiService {

    // ========== JUEGOS ==========

    @GET("games")
    suspend fun getPopularGames(
        @Query("key") apiKey: String,
        @Query("ordering") ordering: String = "-added",
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): GameResponse

    @GET("games")
    suspend fun getTopRatedGames(
        @Query("key") apiKey: String,
        @Query("ordering") ordering: String = "-metacritic",
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): GameResponse

    @GET("games")
    suspend fun getRecentGames(
        @Query("key") apiKey: String,
        @Query("ordering") ordering: String = "-released",
        @Query("dates") dates: String, // formato "2024-01-01,2025-12-31"
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): GameResponse

    @GET("games")
    suspend fun getGamesByGenre(
        @Query("key") apiKey: String,
        @Query("genres") genreId: Int,
        @Query("ordering") ordering: String = "-rating",
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): GameResponse

    @GET("games")
    suspend fun searchGames(
        @Query("key") apiKey: String,
        @Query("search") query: String,
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): GameResponse

    @GET("games/{id}")
    suspend fun getGameDetails(
        @Path("id") gameId: Int,
        @Query("key") apiKey: String
    ): GameDetailDto

    @GET("games/{id}/screenshots")
    suspend fun getGameScreenshots(
        @Path("id") gameId: Int,
        @Query("key") apiKey: String
    ): GameScreenshotsResponse

    // ========== GÉNEROS ==========

    @GET("genres")
    suspend fun getGenres(
        @Query("key") apiKey: String
    ): RawgGenreListResponse

    companion object {
        const val BASE_URL = "https://api.rawg.io/api/"
    }
}
