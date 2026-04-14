package com.example.moviebox.data.local.dao

import androidx.room.*
import com.example.moviebox.data.local.entity.TopItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TopItemDao {

    @Query("SELECT * FROM top_items WHERE userId = :userId ORDER BY mediaType, position ASC")
    fun getAllTopItems(userId: String): Flow<List<TopItemEntity>>

    @Query("SELECT * FROM top_items WHERE userId = :userId AND mediaType = :mediaType ORDER BY position ASC")
    fun getTopItemsByType(userId: String, mediaType: String): Flow<List<TopItemEntity>>

    @Query("SELECT * FROM top_items WHERE userId = :userId AND mediaType = :mediaType AND position = :position LIMIT 1")
    suspend fun getTopItem(userId: String, mediaType: String, position: Int): TopItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopItem(item: TopItemEntity)

    @Query("DELETE FROM top_items WHERE userId = :userId AND mediaType = :mediaType AND position = :position")
    suspend fun deleteTopItem(userId: String, mediaType: String, position: Int)

    @Query("DELETE FROM top_items WHERE userId = :userId")
    suspend fun deleteAllTopItems(userId: String)
}
