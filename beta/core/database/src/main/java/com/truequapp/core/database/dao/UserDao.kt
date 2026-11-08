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

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    fun getUserByEmail(email: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmailSync(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:identifier) OR LOWER(name) = LOWER(:identifier) LIMIT 1")
    suspend fun getUserByIdentifierSync(identifier: String): UserEntity?

    @Query("SELECT * FROM users WHERE (LOWER(email) = LOWER(:email) OR LOWER(name) = LOWER(:email)) AND passwordHash = :passwordHash LIMIT 1")
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

    // =========================================================================
    // CONSULTAS ATÓMICAS Y GESTIÓN DE COINS (UserRepository)
    // =========================================================================

    @Query("SELECT truequeCoins FROM users WHERE LOWER(email) = LOWER(:correo) LIMIT 1")
    suspend fun getCoinsByEmail(correo: String): Double?

    @Query("SELECT truequeCoins FROM users WHERE LOWER(email) = LOWER(:correo) LIMIT 1")
    fun observeCoinsByEmail(correo: String): Flow<Double?>

    @Query("UPDATE users SET truequeCoins = MAX(0.0, truequeCoins - :amount) WHERE LOWER(email) = LOWER(:correo)")
    suspend fun reduceCoins(correo: String, amount: Double): Int

    @Query("UPDATE users SET truequeCoins = truequeCoins + :amount WHERE LOWER(email) = LOWER(:correo)")
    suspend fun addCoins(correo: String, amount: Double): Int

    @Query("UPDATE users SET truequeCoins = :newCoins WHERE LOWER(email) = LOWER(:correo)")
    suspend fun updateCoins(correo: String, newCoins: Double): Int

    // =========================================================================
    // CONSULTAS Y ACTUALIZACIONES DE TRUEQUES REALIZADOS
    // =========================================================================

    @Query("SELECT completedTrades FROM users WHERE LOWER(email) = LOWER(:correo) LIMIT 1")
    suspend fun getCompletedTradesByEmail(correo: String): Int?

    @Query("SELECT completedTrades FROM users WHERE LOWER(email) = LOWER(:correo) LIMIT 1")
    fun observeCompletedTradesByEmail(correo: String): Flow<Int?>

    @Query("UPDATE users SET completedTrades = completedTrades + 1 WHERE LOWER(email) = LOWER(:correo)")
    suspend fun incrementCompletedTrades(correo: String): Int

    @Query("UPDATE users SET completedTrades = :count WHERE LOWER(email) = LOWER(:correo)")
    suspend fun updateCompletedTrades(correo: String, count: Int): Int

    // =========================================================================
    // CONSULTAS Y ACTUALIZACIONES DE CALIFICACIÓN (ESTRELLAS)
    // =========================================================================

    @Query("SELECT bayesianRating FROM users WHERE LOWER(email) = LOWER(:correo) LIMIT 1")
    suspend fun getRatingByEmail(correo: String): Double?

    @Query("SELECT bayesianRating FROM users WHERE LOWER(email) = LOWER(:correo) LIMIT 1")
    fun observeRatingByEmail(correo: String): Flow<Double?>

    @Query("UPDATE users SET bayesianRating = :newRating WHERE LOWER(email) = LOWER(:correo)")
    suspend fun updateRating(correo: String, newRating: Double): Int

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:correo) LIMIT 1")
    fun observeUserByEmail(correo: String): Flow<UserEntity?>
}

