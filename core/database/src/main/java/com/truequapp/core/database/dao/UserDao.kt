package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.truequapp.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para acceso y gestión de usuarios y autenticación en la base de datos local.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserByIdSync(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    fun getUserByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmailSync(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email AND passwordHash = :passwordHash LIMIT 1")
    suspend fun authenticate(email: String, passwordHash: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET truequeCoins = :availableCoins, escrowCoins = :escrowCoins WHERE id = :userId")
    suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double)

    @Query("SELECT truequeCoins FROM users WHERE id = :userId LIMIT 1")
    suspend fun getTruequeCoins(userId: String): Double?

    @Query("SELECT * FROM users ORDER BY createdAtEpochMs DESC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}
