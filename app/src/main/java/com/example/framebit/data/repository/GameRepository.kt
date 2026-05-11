package com.example.framebit.data.repository

import com.example.framebit.data.local.dao.GameDao
import com.example.framebit.data.local.entity.GameEntity
import com.example.framebit.data.remote.api.RawgApiService
import com.example.framebit.data.remote.dto.GameDetailDto
import com.example.framebit.data.remote.dto.GameDto
import com.example.framebit.data.remote.dto.RawgGenreDto
import com.example.framebit.data.remote.dto.ScreenshotDto

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameDao: GameDao,
    private val apiService: RawgApiService
) {

    // ========== OPERACIONES LOCALES ==========

    fun getAllGames(): Flow<List<GameEntity>> = gameDao.getAllGames()
    fun getWatchlistedGames(): Flow<List<GameEntity>> = gameDao.getWatchlistedGames()
    fun getFavoriteGames(): Flow<List<GameEntity>> = gameDao.getFavoriteGames()
    fun getPlayedGames(): Flow<List<GameEntity>> = gameDao.getPlayedGames()
    fun searchGamesLocal(query: String): Flow<List<GameEntity>> = gameDao.searchGames(query)
    suspend fun getGameById(gameId: Int): GameEntity? = gameDao.getGameById(gameId)
    suspend fun insertGame(game: GameEntity) = gameDao.insertGame(game)
    suspend fun updateGame(game: GameEntity) = gameDao.updateGame(game)
    suspend fun toggleWatchlisted(gameId: Int, isWatchlisted: Boolean) = gameDao.updateWatchlistedStatus(gameId, isWatchlisted)
    suspend fun toggleFavorite(gameId: Int, isFavorite: Boolean) = gameDao.updateFavoriteStatus(gameId, isFavorite)
    suspend fun togglePlayed(gameId: Int, isPlayed: Boolean) = gameDao.updatePlayedStatus(gameId, isPlayed)
    suspend fun deleteGame(gameId: Int) = gameDao.deleteGameById(gameId)

    // ========== OPERACIONES REMOTAS ==========

    suspend fun getPopularGamesFromApi(apiKey: String): Result<List<GameDto>> {
        return try { Result.success(apiService.getPopularGames(apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getTopRatedGamesFromApi(apiKey: String): Result<List<GameDto>> {
        return try { Result.success(apiService.getTopRatedGames(apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getRecentGamesFromApi(apiKey: String, dates: String): Result<List<GameDto>> {
        return try { Result.success(apiService.getRecentGames(apiKey, dates = dates).results) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun searchGamesFromApi(apiKey: String, query: String): Result<List<GameDto>> {
        return try { Result.success(apiService.searchGames(apiKey, query).results) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getGameDetailsFromApi(apiKey: String, gameId: Int): Result<GameDetailDto> {
        return try { Result.success(apiService.getGameDetails(gameId, apiKey)) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getGameScreenshots(apiKey: String, gameId: Int): Result<List<ScreenshotDto>> {
        return try { Result.success(apiService.getGameScreenshots(gameId, apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getGamesByGenre(apiKey: String, genreId: Int): Result<List<GameDto>> {
        return try { Result.success(apiService.getGamesByGenre(apiKey, genreId).results) } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getGenres(apiKey: String): Result<List<RawgGenreDto>> {
        return try { Result.success(apiService.getGenres(apiKey).results) } catch (e: Exception) { Result.failure(e) }
    }
}