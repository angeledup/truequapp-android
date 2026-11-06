package com.truequapp.core.database.repository

import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.datastore.UserSessionDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio centralizado para consultar y actualizar la información del usuario en la base de datos SQL.
 * Garantiza que la base de datos sea la Fuente Única de la Verdad (Single Source of Truth) para:
 * 1. TruequeCoins (consulta, reducción, incremento y observación reactiva).
 * 2. Trueques realizados (consulta e incremento).
 * 3. Calificación y estrellas.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val userSessionDataStore: UserSessionDataStore
) {

    /**
     * Resuelve el correo del usuario activo si no se proporciona uno explícitamente.
     */
    private suspend fun resolveEmail(explicitEmail: String?): String? {
        if (!explicitEmail.isNullOrBlank()) return explicitEmail.trim()
        val session = userSessionDataStore.sessionFlow.first()
        return session.userEmail?.trim()
    }

    // =========================================================================
    // TRUEQUECOINS
    // =========================================================================

    /**
     * Checa la cantidad de TruequeCoins y devuelve el saldo actual del usuario desde la base de datos.
     * Retorna 0.0 si el usuario no existe o no tiene saldo registrado.
     */
    suspend fun getCoins(email: String? = null): Double {
        val targetEmail = resolveEmail(email) ?: return 0.0
        return userDao.getCoinsByEmail(targetEmail) ?: 0.0
    }

    /**
     * Reduce una cantidad específica de TruequeCoins en la base de datos.
     * Retorna true si la operación se aplicó exitosamente en la base de datos.
     */
    suspend fun reduceCoins(amount: Double, email: String? = null): Boolean {
        require(amount >= 0.0) { "El monto a reducir no puede ser negativo." }
        val targetEmail = resolveEmail(email) ?: return false
        val rowsAffected = userDao.reduceCoins(targetEmail, amount)
        return rowsAffected > 0
    }

    /**
     * Suma / abona una cantidad específica de TruequeCoins al usuario en la base de datos.
     */
    suspend fun addCoins(amount: Double, email: String? = null): Boolean {
        require(amount >= 0.0) { "El monto a añadir no puede ser negativo." }
        val targetEmail = resolveEmail(email) ?: return false
        val rowsAffected = userDao.addCoins(targetEmail, amount)
        return rowsAffected > 0
    }

    /**
     * Establece un saldo fijo absoluto de TruequeCoins en la base de datos.
     */
    suspend fun updateCoins(newCoins: Double, email: String? = null): Boolean {
        require(newCoins >= 0.0) { "El saldo no puede ser negativo." }
        val targetEmail = resolveEmail(email) ?: return false
        val rowsAffected = userDao.updateCoins(targetEmail, newCoins)
        return rowsAffected > 0
    }

    /**
     * Observa en tiempo real las TruequeCoins del usuario. Cualquier cambio en la BD
     * se propaga automáticamente a las pantallas suscritas.
     */
    fun observeCoins(email: String? = null): Flow<Double> {
        return if (!email.isNullOrBlank()) {
            userDao.observeCoinsByEmail(email.trim()).map { it ?: 0.0 }
        } else {
            userSessionDataStore.sessionFlow.flatMapLatest { session ->
                val activeEmail = session.userEmail
                if (!activeEmail.isNullOrBlank()) {
                    userDao.observeCoinsByEmail(activeEmail).map { it ?: 0.0 }
                } else {
                    emptyFlow()
                }
            }
        }
    }

    // =========================================================================
    // TRUEQUES REALIZADOS
    // =========================================================================

    /**
     * Checa la cantidad de trueques realizados por el usuario en la base de datos.
     */
    suspend fun getCompletedTrades(email: String? = null): Int {
        val targetEmail = resolveEmail(email) ?: return 0
        return userDao.getCompletedTradesByEmail(targetEmail) ?: 0
    }

    /**
     * Incrementa en 1 la cantidad de trueques realizados en la base de datos.
     */
    suspend fun incrementCompletedTrades(email: String? = null): Boolean {
        val targetEmail = resolveEmail(email) ?: return false
        val rowsAffected = userDao.incrementCompletedTrades(targetEmail)
        return rowsAffected > 0
    }

    /**
     * Actualiza el valor explícito de trueques completados.
     */
    suspend fun updateCompletedTrades(count: Int, email: String? = null): Boolean {
        require(count >= 0) { "El contador de trueques no puede ser negativo." }
        val targetEmail = resolveEmail(email) ?: return false
        val rowsAffected = userDao.updateCompletedTrades(targetEmail, count)
        return rowsAffected > 0
    }

    /**
     * Observa en tiempo real los trueques realizados por el usuario.
     */
    fun observeCompletedTrades(email: String? = null): Flow<Int> {
        return if (!email.isNullOrBlank()) {
            userDao.observeCompletedTradesByEmail(email.trim()).map { it ?: 0 }
        } else {
            userSessionDataStore.sessionFlow.flatMapLatest { session ->
                val activeEmail = session.userEmail
                if (!activeEmail.isNullOrBlank()) {
                    userDao.observeCompletedTradesByEmail(activeEmail).map { it ?: 0 }
                } else {
                    emptyFlow()
                }
            }
        }
    }

    // =========================================================================
    // CALIFICACIÓN (ESTRELLAS)
    // =========================================================================

    /**
     * Checa la calificación / estrellas del usuario en la base de datos.
     */
    suspend fun getRating(email: String? = null): Double {
        val targetEmail = resolveEmail(email) ?: return 5.0
        return userDao.getRatingByEmail(targetEmail) ?: 5.0
    }

    /**
     * Actualiza la calificación en la base de datos.
     */
    suspend fun updateRating(newRating: Double, email: String? = null): Boolean {
        require(newRating >= 0.0) { "La calificación no puede ser negativa." }
        val targetEmail = resolveEmail(email) ?: return false
        val rowsAffected = userDao.updateRating(targetEmail, newRating)
        return rowsAffected > 0
    }

    /**
     * Observa en tiempo real la calificación del usuario.
     */
    fun observeRating(email: String? = null): Flow<Double> {
        return if (!email.isNullOrBlank()) {
            userDao.observeRatingByEmail(email.trim()).map { it ?: 5.0 }
        } else {
            userSessionDataStore.sessionFlow.flatMapLatest { session ->
                val activeEmail = session.userEmail
                if (!activeEmail.isNullOrBlank()) {
                    userDao.observeRatingByEmail(activeEmail).map { it ?: 5.0 }
                } else {
                    emptyFlow()
                }
            }
        }
    }

    // =========================================================================
    // USUARIO COMPLETO Y OBSERVACIÓN GENERAL
    // =========================================================================

    /**
     * Obtiene la entidad completa del usuario actual desde la base de datos.
     */
    suspend fun getCurrentUser(): UserEntity? {
        val email = resolveEmail(null) ?: return null
        return userDao.getUserByEmail(email)
    }

    /**
     * Observa el perfil del usuario activo en tiempo real ante cualquier cambio en la tabla usuarios.
     */
    fun observeCurrentUser(): Flow<UserEntity?> {
        return userSessionDataStore.sessionFlow.flatMapLatest { session ->
            val email = session.userEmail
            if (!email.isNullOrBlank()) {
                userDao.observeUserByEmail(email)
            } else {
                emptyFlow()
            }
        }
    }
}

