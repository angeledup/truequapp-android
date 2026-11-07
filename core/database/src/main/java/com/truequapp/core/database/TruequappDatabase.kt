package com.truequapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.truequapp.core.database.dao.ChatMessageDao
import com.truequapp.core.database.dao.TruequeDao
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.ChatMessageEntity
import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.TruequeEntity
import com.truequapp.core.database.entity.UserEntity

/**
 * Base de datos principal de Truequapp gestionada mediante SQLite y Room.
 */
@Database(
    entities = [
        UserEntity::class,
        OfertaItemEntity::class,
        TruequeEntity::class,
        ChatMessageEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TruequappDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun truequeDao(): TruequeDao
    abstract fun chatMessageDao(): ChatMessageDao
}
