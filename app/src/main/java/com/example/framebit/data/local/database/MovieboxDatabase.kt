package com.example.framebit.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.framebit.data.local.dao.GameDao
import com.example.framebit.data.local.dao.MovieDao
import com.example.framebit.data.local.dao.ReviewDao
import com.example.framebit.data.local.dao.TopItemDao
import com.example.framebit.data.local.dao.TvShowDao
import com.example.framebit.data.local.dao.UserDao
import com.example.framebit.data.local.entity.GameEntity
import com.example.framebit.data.local.entity.MovieEntity
import com.example.framebit.data.local.entity.ReviewEntity
import com.example.framebit.data.local.entity.TopItemEntity
import com.example.framebit.data.local.entity.TvShowEntity
import com.example.framebit.data.local.entity.UserEntity

@Database(
    entities = [
        MovieEntity::class,
        TvShowEntity::class,
        ReviewEntity::class,
        UserEntity::class,
        GameEntity::class,
        TopItemEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class MovieBoxDatabase : RoomDatabase() {

    abstract fun movieDao(): MovieDao
    abstract fun tvShowDao(): TvShowDao
    abstract fun reviewDao(): ReviewDao
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun topItemDao(): TopItemDao

    companion object {
        @Volatile
        private var INSTANCE: MovieBoxDatabase? = null

        fun getDatabase(context: Context): MovieBoxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovieBoxDatabase::class.java,
                    "moviebox_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
