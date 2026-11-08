package com.truequapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.truequapp.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interfaz de Acceso a Datos (DAO) con consultas SQL explícitas para la tabla 'usuarios'.
 * Fuente única de la verdad (Single Source of Truth) para el saldo en TruequeCoins,
 * estadísticas de trueques realizados y calificación del usuario.
 */
@Dao
interface UserDao {

    /**
     * Consulta SQL para obtener un usuario a partir de su correo electrónico.
     */
    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    suspend fun getUserByEmail(correo: String): UserEntity?

    /**
     * Consulta SQL reactiva para observar cambios en un usuario en tiempo real.
     */
    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    fun observeUserByEmail(correo: String): Flow<UserEntity?>

    /**
     * Consulta SQL para verificar la existencia previa de un correo en el registro.
     */
    @Query("SELECT COUNT(*) FROM usuarios WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun countByEmail(correo: String): Int

    /**
     * Sentencia SQL INSERT para persistir una nueva cuenta con sus 7 atributos.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    /**
     * Actualiza la entidad completa de usuario.
     */
    @Update
    suspend fun updateUser(user: UserEntity)

    /**
     * Consulta SQL para listar la totalidad de cuentas registradas en la base de datos local.
     */
    @Query("SELECT * FROM usuarios")
    fun getAllUsers(): Flow<List<UserEntity>>

    // =========================================================================
    // CONSULTAS ATÓMICAS Y GESTIÓN DE COINS (Single Source of Truth)
    // =========================================================================

    /**
     * Checa y devuelve la cantidad de TruequeCoins que tiene el usuario en la base de datos.
     */
    @Query("SELECT cantidad_trueque_coins FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    suspend fun getCoinsByEmail(correo: String): Double?

    /**
     * Observa en tiempo real la cantidad de TruequeCoins del usuario ante cualquier modificación.
     */
    @Query("SELECT cantidad_trueque_coins FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    fun observeCoinsByEmail(correo: String): Flow<Double?>

    /**
     * Reduce una cantidad específica de TruequeCoins de forma atómica en la base de datos.
     */
    @Query("UPDATE usuarios SET cantidad_trueque_coins = MAX(0.0, cantidad_trueque_coins - :amount) WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun reduceCoins(correo: String, amount: Double): Int

    /**
     * Incrementa / añade una cantidad específica de TruequeCoins al saldo del usuario en la base de datos.
     */
    @Query("UPDATE usuarios SET cantidad_trueque_coins = cantidad_trueque_coins + :amount WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun addCoins(correo: String, amount: Double): Int

    /**
     * Actualiza directamente el saldo absoluto de TruequeCoins de un usuario.
     */
    @Query("UPDATE usuarios SET cantidad_trueque_coins = :newCoins WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun updateCoins(correo: String, newCoins: Double): Int

    // =========================================================================
    // CONSULTAS Y ACTUALIZACIONES DE TRUEQUES REALIZADOS
    // =========================================================================

    /**
     * Consulta y devuelve la cantidad de trueques realizados por el usuario.
     */
    @Query("SELECT trueques_realizados FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    suspend fun getCompletedTradesByEmail(correo: String): Int?

    /**
     * Observa en tiempo real los trueques realizados por el usuario.
     */
    @Query("SELECT trueques_realizados FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    fun observeCompletedTradesByEmail(correo: String): Flow<Int?>

    /**
     * Incrementa en 1 la cantidad de trueques completados en la base de datos.
     */
    @Query("UPDATE usuarios SET trueques_realizados = trueques_realizados + 1 WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun incrementCompletedTrades(correo: String): Int

    /**
     * Actualiza el contador explícito de trueques realizados por el usuario.
     */
    @Query("UPDATE usuarios SET trueques_realizados = :count WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun updateCompletedTrades(correo: String, count: Int): Int

    // =========================================================================
    // CONSULTAS Y ACTUALIZACIONES DE CALIFICACIÓN (ESTRELLAS)
    // =========================================================================

    /**
     * Consulta y devuelve la calificación actual del usuario.
     */
    @Query("SELECT calificacion FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    suspend fun getRatingByEmail(correo: String): Double?

    /**
     * Observa en tiempo real la calificación del usuario.
     */
    @Query("SELECT calificacion FROM usuarios WHERE LOWER(correo) = LOWER(:correo) LIMIT 1")
    fun observeRatingByEmail(correo: String): Flow<Double?>

    /**
     * Actualiza la calificación / estrellas del usuario en la base de datos.
     */
    @Query("UPDATE usuarios SET calificacion = :newRating WHERE LOWER(correo) = LOWER(:correo)")
    suspend fun updateRating(correo: String, newRating: Double): Int

    // =========================================================================
    // MÉTODOS DE COMPATIBILIDAD CON OTRAS CAPAS
    // =========================================================================

    @Query("SELECT * FROM usuarios WHERE LOWER(usuario) = LOWER(:id) OR LOWER(correo) = LOWER(:id) LIMIT 1")
    fun getUserById(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM usuarios WHERE LOWER(usuario) = LOWER(:id) OR LOWER(correo) = LOWER(:id) LIMIT 1")
    suspend fun getUserByIdSync(id: String): UserEntity?

    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmailSync(email: String): UserEntity? = getUserByEmail(email)

    @Query("SELECT * FROM usuarios WHERE LOWER(correo) = LOWER(:identifier) OR LOWER(usuario) = LOWER(:identifier) LIMIT 1")
    suspend fun getUserByIdentifierSync(identifier: String): UserEntity?

    @Query("SELECT * FROM usuarios WHERE (LOWER(correo) = LOWER(:email) OR LOWER(usuario) = LOWER(:email)) AND contrasena = :passwordHash LIMIT 1")
    suspend fun authenticate(email: String, passwordHash: String): UserEntity?

    @Query("UPDATE usuarios SET cantidad_trueque_coins = :availableCoins WHERE (LOWER(usuario) = LOWER(:userId) OR LOWER(correo) = LOWER(:userId)) AND (:escrowCoins IS NOT NULL OR 1=1)")
    suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double)

    @Query("SELECT cantidad_trueque_coins FROM usuarios WHERE LOWER(usuario) = LOWER(:userId) OR LOWER(correo) = LOWER(:userId) LIMIT 1")
    suspend fun getTruequeCoins(userId: String): Double?

    @Query("DELETE FROM usuarios WHERE LOWER(usuario) = LOWER(:userId) OR LOWER(correo) = LOWER(:userId)")
    suspend fun deleteUser(userId: String)
}
