package com.example.moviebox.data.local.dao

import androidx.room.*
import com.example.moviebox.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {

    @Query("SELECT * FROM games ORDER BY addedDate DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isWatchlisted = 1 ORDER BY addedDate DESC")
    fun getWatchlistedGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isFavorite = 1 ORDER BY addedDate DESC")
    fun getFavoriteGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE isPlayed = 1 ORDER BY addedDate DESC")
    fun getPlayedGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :gameId")
    suspend fun getGameById(gameId: Int): GameEntity?

    @Query("SELECT * FROM games WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchGames(query: String): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("UPDATE games SET isWatchlisted = :isWatchlisted WHERE id = :gameId")
    suspend fun updateWatchlistedStatus(gameId: Int, isWatchlisted: Boolean)

    @Query("UPDATE games SET isFavorite = :isFavorite WHERE id = :gameId")
    suspend fun updateFavoriteStatus(gameId: Int, isFavorite: Boolean)

    @Query("UPDATE games SET isPlayed = :isPlayed WHERE id = :gameId")
    suspend fun updatePlayedStatus(gameId: Int, isPlayed: Boolean)

    @Delete
    suspend fun deleteGame(game: GameEntity)

    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: Int)
}