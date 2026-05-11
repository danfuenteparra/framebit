package com.example.framebit.data.repository

import com.example.framebit.data.local.dao.MovieDao
import com.example.framebit.data.local.entity.MovieEntity
import com.example.framebit.data.remote.api.TmdbApiService
import com.example.framebit.data.remote.dto.CreditsResponse
import com.example.framebit.data.remote.dto.GenreDto
import com.example.framebit.data.remote.dto.MovieDetailDto
import com.example.framebit.data.remote.dto.MovieDto
import com.example.framebit.data.remote.dto.PersonCombinedCreditsResponse
import com.example.framebit.data.remote.dto.PersonDto
import com.example.framebit.data.remote.dto.VideosResponse
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MovieRepository @Inject constructor(
    private val movieDao: MovieDao,
    private val apiService: TmdbApiService
) {

    // ========== OPERACIONES LOCALES ==========

    fun getAllMovies(): Flow<List<MovieEntity>> = movieDao.getAllMovies()
    fun getWatchlistedMovies(): Flow<List<MovieEntity>> = movieDao.getWatchlistedMovies()
    fun getFavoriteMovies(): Flow<List<MovieEntity>> = movieDao.getFavoriteMovies()
    fun getWatchedMovies(): Flow<List<MovieEntity>> = movieDao.getWatchedMovies()
    fun searchMovies(query: String): Flow<List<MovieEntity>> = movieDao.searchMovies(query)
    suspend fun getMovieById(movieId: Int): MovieEntity? = movieDao.getMovieById(movieId)
    suspend fun insertMovie(movie: MovieEntity) = movieDao.insertMovie(movie)
    suspend fun updateMovie(movie: MovieEntity) = movieDao.updateMovie(movie)
    suspend fun toggleWatchlisted(movieId: Int, isWatchlisted: Boolean) = movieDao.updateWatchlistedStatus(movieId, isWatchlisted)
    suspend fun toggleFavorite(movieId: Int, isFavorite: Boolean) = movieDao.updateFavoriteStatus(movieId, isFavorite)
    suspend fun toggleWatched(movieId: Int, isWatched: Boolean) = movieDao.updateWatchedStatus(movieId, isWatched)
    suspend fun deleteMovie(movieId: Int) = movieDao.deleteMovieById(movieId)

    // ========== OPERACIONES REMOTAS ==========

    suspend fun getPopularMoviesFromApi(apiKey: String): Result<List<MovieDto>> {
        return try { Result.success(apiService.getPopularMovies(apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getTopRatedMoviesFromApi(apiKey: String): Result<List<MovieDto>> {
        return try { Result.success(apiService.getTopRatedMovies(apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getNowPlayingMoviesFromApi(apiKey: String): Result<List<MovieDto>> {
        return try { Result.success(apiService.getNowPlayingMovies(apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun searchMoviesFromApi(apiKey: String, query: String): Result<List<MovieDto>> {
        return try { Result.success(apiService.searchMovies(apiKey, query).results) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getMovieDetailsFromApi(apiKey: String, movieId: Int): Result<MovieDetailDto> {
        return try { Result.success(apiService.getMovieDetails(movieId, apiKey)) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getMovieCredits(apiKey: String, movieId: Int): Result<CreditsResponse> {
        return try { Result.success(apiService.getMovieCredits(movieId, apiKey)) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getMovieVideos(apiKey: String, movieId: Int): Result<VideosResponse> {
        return try { Result.success(apiService.getMovieVideos(movieId, apiKey)) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun searchPerson(apiKey: String, query: String): Result<List<PersonDto>> {
        return try { Result.success(apiService.searchPerson(apiKey, query).results) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getPersonCombinedCredits(apiKey: String, personId: Int): Result<PersonCombinedCreditsResponse> {
        return try { Result.success(apiService.getPersonCombinedCredits(personId, apiKey)) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getMovieGenres(apiKey: String): Result<List<GenreDto>> {
        return try { Result.success(apiService.getMovieGenres(apiKey).genres) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun discoverMoviesByGenre(apiKey: String, genreId: Int): Result<List<MovieDto>> {
        return try { Result.success(apiService.discoverMoviesByGenre(apiKey, genreId).results) } catch (e: Exception) { Result.failure(e) }
    }
}