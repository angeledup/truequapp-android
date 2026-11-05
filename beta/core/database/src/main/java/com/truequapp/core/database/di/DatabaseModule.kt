package com.truequapp.core.database.di

import android.content.Context
import com.truequapp.core.database.TruequappDatabase
import com.truequapp.core.database.dao.ChatMessageDao
import com.truequapp.core.database.dao.LedgerEntryDao
import com.truequapp.core.database.dao.SafeZoneDao
import com.truequapp.core.database.dao.TradeItemDao
import com.truequapp.core.database.dao.TradeProposalDao
import com.truequapp.core.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Inyección de Dependencias Hilt para la Base de Datos SQLite Room y sus DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideTruequappDatabase(
        @ApplicationContext context: Context
    ): TruequappDatabase {
        return TruequappDatabase.buildDatabase(context)
    }

    @Provides
    @Singleton
    fun provideUserDao(database: TruequappDatabase): UserDao = database.userDao()

    @Provides
    @Singleton
    fun provideTradeItemDao(database: TruequappDatabase): TradeItemDao = database.tradeItemDao()

    @Provides
    @Singleton
    fun provideTradeProposalDao(database: TruequappDatabase): TradeProposalDao = database.tradeProposalDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(database: TruequappDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    @Singleton
    fun provideLedgerEntryDao(database: TruequappDatabase): LedgerEntryDao = database.ledgerEntryDao()

    @Provides
    @Singleton
    fun provideSafeZoneDao(database: TruequappDatabase): SafeZoneDao = database.safeZoneDao()
}
