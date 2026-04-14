package com.example.moviebox.data.local.dao

import androidx.room.*
import com.example.moviebox.data.local.entity.MovieEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM movies ORDER BY addedDate DESC")
    fun getAllMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isWatchlisted = 1 ORDER BY addedDate DESC")
    fun getWatchlistedMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isFavorite = 1 ORDER BY addedDate DESC")
    fun getFavoriteMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE isWatched = 1 ORDER BY addedDate DESC")
    fun getWatchedMovies(): Flow<List<MovieEntity>>

    @Query("SELECT * FROM movies WHERE id = :movieId")
    suspend fun getMovieById(movieId: Int): MovieEntity?

    @Query("SELECT * FROM movies WHERE title LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchMovies(query: String): Flow<List<MovieEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovie(movie: MovieEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovies(movies: List<MovieEntity>)

    @Update
    suspend fun updateMovie(movie: MovieEntity)

    @Query("UPDATE movies SET isWatchlisted = :isWatchlisted WHERE id = :movieId")
    suspend fun updateWatchlistedStatus(movieId: Int, isWatchlisted: Boolean)

    @Query("UPDATE movies SET isFavorite = :isFavorite WHERE id = :movieId")
    suspend fun updateFavoriteStatus(movieId: Int, isFavorite: Boolean)

    @Query("UPDATE movies SET isWatched = :isWatched WHERE id = :movieId")
    suspend fun updateWatchedStatus(movieId: Int, isWatched: Boolean)

    @Delete
    suspend fun deleteMovie(movie: MovieEntity)

    @Query("DELETE FROM movies WHERE id = :movieId")
    suspend fun deleteMovieById(movieId: Int)

    @Query("DELETE FROM movies")
    suspend fun deleteAllMovies()
}