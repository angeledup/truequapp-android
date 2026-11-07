package com.truequapp.core.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.truequapp.core.database.TruequappDatabase
import com.truequapp.core.database.dao.ChatMessageDao
import com.truequapp.core.database.dao.TruequeDao
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
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    insertDefaultAdminUser(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    insertDefaultAdminUser(db)
                }

                private fun insertDefaultAdminUser(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        INSERT OR REPLACE INTO usuarios (
                            correo,
                            contrasena,
                            usuario,
                            numero_telefonico,
                            cantidad_trueque_coins,
                            trueques_realizados,
                            calificacion
                        ) VALUES (
                            'angeleduvr@gmail.com',
                            '123456',
                            'Admin_AEVR',
                            '+529212404565',
                            100000000.0,
                            10,
                            10.0
                        )
                        """.trimIndent()
                    )
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: TruequappDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideTruequeDao(database: TruequappDatabase): TruequeDao {
        return database.truequeDao()
    }

    @Provides
    @Singleton
    fun provideChatMessageDao(database: TruequappDatabase): ChatMessageDao {
        return database.chatMessageDao()
    }
}
