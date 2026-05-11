package com.example.framebit.data.local.dao

import androidx.room.*
import com.example.framebit.data.local.entity.TvShowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TvShowDao {

    @Query("SELECT * FROM tv_shows ORDER BY addedDate DESC")
    fun getAllTvShows(): Flow<List<TvShowEntity>>

    @Query("SELECT * FROM tv_shows WHERE isWatchlisted = 1 ORDER BY addedDate DESC")
    fun getWatchlistedTvShows(): Flow<List<TvShowEntity>>

    @Query("SELECT * FROM tv_shows WHERE isFavorite = 1 ORDER BY addedDate DESC")
    fun getFavoriteTvShows(): Flow<List<TvShowEntity>>

    @Query("SELECT * FROM tv_shows WHERE isWatched = 1 ORDER BY addedDate DESC")
    fun getWatchedTvShows(): Flow<List<TvShowEntity>>

    @Query("SELECT * FROM tv_shows WHERE id = :tvShowId")
    suspend fun getTvShowById(tvShowId: Int): TvShowEntity?

    @Query("SELECT * FROM tv_shows WHERE name LIKE '%' || :query || '%'")
    fun searchTvShows(query: String): Flow<List<TvShowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTvShow(tvShow: TvShowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTvShows(tvShows: List<TvShowEntity>)

    @Update
    suspend fun updateTvShow(tvShow: TvShowEntity)

    @Query("UPDATE tv_shows SET isWatchlisted = :isWatchlisted WHERE id = :tvShowId")
    suspend fun updateWatchlistedStatus(tvShowId: Int, isWatchlisted: Boolean)

    @Query("UPDATE tv_shows SET isFavorite = :isFavorite WHERE id = :tvShowId")
    suspend fun updateFavoriteStatus(tvShowId: Int, isFavorite: Boolean)

    @Query("UPDATE tv_shows SET isWatched = :isWatched WHERE id = :tvShowId")
    suspend fun updateWatchedStatus(tvShowId: Int, isWatched: Boolean)

    @Delete
    suspend fun deleteTvShow(tvShow: TvShowEntity)

    @Query("DELETE FROM tv_shows WHERE id = :tvShowId")
    suspend fun deleteTvShowById(tvShowId: Int)
}