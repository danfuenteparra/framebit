package com.example.framebit.di

import android.content.Context
import com.example.framebit.data.local.dao.GameDao
import com.example.framebit.data.local.dao.MovieDao
import com.example.framebit.data.local.dao.ReviewDao
import com.example.framebit.data.local.dao.TopItemDao
import com.example.framebit.data.local.dao.TvShowDao
import com.example.framebit.data.local.dao.UserDao
import com.example.framebit.data.local.database.MovieBoxDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMovieBoxDatabase(
        @ApplicationContext context: Context
    ): MovieBoxDatabase {
        return MovieBoxDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideMovieDao(database: MovieBoxDatabase): MovieDao {
        return database.movieDao()
    }

    @Provides
    @Singleton
    fun provideTvShowDao(database: MovieBoxDatabase): TvShowDao {
        return database.tvShowDao()
    }

    @Provides
    @Singleton
    fun provideReviewDao(database: MovieBoxDatabase): ReviewDao {
        return database.reviewDao()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: MovieBoxDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideGameDao(database: MovieBoxDatabase): GameDao {
        return database.gameDao()
    }

    @Provides
    @Singleton
    fun provideTopItemDao(database: MovieBoxDatabase): TopItemDao {
        return database.topItemDao()
    }
}
