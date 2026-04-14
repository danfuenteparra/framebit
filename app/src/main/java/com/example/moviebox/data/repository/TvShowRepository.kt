package com.example.moviebox.data.repository

import com.example.moviebox.data.local.dao.TvShowDao
import com.example.moviebox.data.local.entity.TvShowEntity
import com.example.moviebox.data.remote.api.TmdbApiService
import com.example.moviebox.data.remote.dto.CreditsResponse
import com.example.moviebox.data.remote.dto.GenreDto
import com.example.moviebox.data.remote.dto.TvShowDetailDto
import com.example.moviebox.data.remote.dto.TvShowDto
import com.example.moviebox.data.remote.dto.VideosResponse
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvShowRepository @Inject constructor(
    private val tvShowDao: TvShowDao,
    private val apiService: TmdbApiService
) {

    // ========== OPERACIONES LOCALES ==========

    fun getAllTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getAllTvShows()
    fun getWatchlistedTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getWatchlistedTvShows()
    fun getFavoriteTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getFavoriteTvShows()
    fun getWatchedTvShows(): Flow<List<TvShowEntity>> = tvShowDao.getWatchedTvShows()
    fun searchTvShows(query: String): Flow<List<TvShowEntity>> = tvShowDao.searchTvShows(query)
    suspend fun getTvShowById(tvShowId: Int): TvShowEntity? = tvShowDao.getTvShowById(tvShowId)
    suspend fun insertTvShow(tvShow: TvShowEntity) = tvShowDao.insertTvShow(tvShow)
    suspend fun updateTvShow(tvShow: TvShowEntity) = tvShowDao.updateTvShow(tvShow)
    suspend fun toggleWatchlisted(tvShowId: Int, isWatchlisted: Boolean) = tvShowDao.updateWatchlistedStatus(tvShowId, isWatchlisted)
    suspend fun toggleFavorite(tvShowId: Int, isFavorite: Boolean) = tvShowDao.updateFavoriteStatus(tvShowId, isFavorite)
    suspend fun toggleWatched(tvShowId: Int, isWatched: Boolean) = tvShowDao.updateWatchedStatus(tvShowId, isWatched)
    suspend fun deleteTvShow(tvShowId: Int) = tvShowDao.deleteTvShowById(tvShowId)

    // ========== OPERACIONES REMOTAS ==========

    suspend fun getPopularTvShowsFromApi(apiKey: String): Result<List<TvShowDto>> {
        return try { Result.success(apiService.getPopularTvShows(apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getTopRatedTvShowsFromApi(apiKey: String): Result<List<TvShowDto>> {
        return try { Result.success(apiService.getTopRatedTvShows(apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun searchTvShowsFromApi(apiKey: String, query: String): Result<List<TvShowDto>> {
        return try { Result.success(apiService.searchTvShows(apiKey, query).results) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getTvShowDetailsFromApi(apiKey: String, tvShowId: Int): Result<TvShowDetailDto> {
        return try { Result.success(apiService.getTvShowDetails(tvShowId, apiKey)) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getTvShowCredits(apiKey: String, tvShowId: Int): Result<CreditsResponse> {
        return try { Result.success(apiService.getTvShowCredits(tvShowId, apiKey)) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getTvShowVideos(apiKey: String, tvShowId: Int): Result<VideosResponse> {
        return try { Result.success(apiService.getTvShowVideos(tvShowId, apiKey)) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun getTvShowGenres(apiKey: String): Result<List<GenreDto>> {
        return try { Result.success(apiService.getTvShowGenres(apiKey).genres) } catch (e: Exception) { Result.failure(e) }
    }
    suspend fun discoverTvShowsByGenre(apiKey: String, genreId: Int): Result<List<TvShowDto>> {
        return try { Result.success(apiService.discoverTvShowsByGenre(apiKey, genreId).results) } catch (e: Exception) { Result.failure(e) }
    }
}