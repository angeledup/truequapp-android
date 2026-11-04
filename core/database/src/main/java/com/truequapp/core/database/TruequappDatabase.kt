package com.truequapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity

/**
 * Base de datos principal de Truequapp gestionada mediante SQLite y Room.
 */
@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TruequappDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

