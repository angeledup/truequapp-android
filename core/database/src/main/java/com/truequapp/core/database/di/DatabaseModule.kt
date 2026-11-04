package com.truequapp.core.database.di

import android.content.Context
import androidx.room.Room
import com.truequapp.core.database.TruequappDatabase
import com.truequapp.core.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "truequapp_database.db"

    @Provides
    @Singleton
    fun provideTruequappDatabase(
        @ApplicationContext context: Context
    ): TruequappDatabase {
        return Room.databaseBuilder(
            context,
            TruequappDatabase::class.java,
            DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: TruequappDatabase): UserDao {
        return database.userDao()
    }
}

