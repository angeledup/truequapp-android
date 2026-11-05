package com.truequapp.auth.data

import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LoginResult {
    data class Success(val user: UserEntity) : LoginResult
    data class UserNotFound(val message: String = "No hay una cuenta vinculada al correo") : LoginResult
    data class InvalidPassword(val message: String = "Contraseña incorrecta") : LoginResult
    data class ValidationError(val message: String) : LoginResult
}

sealed interface RegisterResult {
    data class Success(val user: UserEntity) : RegisterResult
    data class EmailAlreadyExists(val message: String = "Ya existe una cuenta registrada con este correo") : RegisterResult
    data class ValidationError(val message: String) : RegisterResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val userSessionDataStore: UserSessionDataStore
) {

    val currentSessionFlow: Flow<UserSession> get() = userSessionDataStore.sessionFlow

    /**
     * Valida credenciales contra la base de datos SQL.
     * Retorna error específico si el correo no existe ("No hay una cuenta vinculada al correo")
     * o si la contraseña no coincide ("Contraseña incorrecta").
     */
    suspend fun login(correo: String, contrasena: String): LoginResult {
        val trimmedEmail = correo.trim()

        if (trimmedEmail.isBlank()) {
            return LoginResult.ValidationError("Por favor ingresa tu correo electrónico")
        }
        if (contrasena.isBlank()) {
            return LoginResult.ValidationError("Por favor ingresa tu contraseña")
        }

        val user = userDao.getUserByEmail(trimmedEmail)
            ?: return LoginResult.UserNotFound()

        if (user.contrasena != contrasena) {
            return LoginResult.InvalidPassword()
        }

        // Al autenticar con éxito, se persiste la sesión del usuario
        userSessionDataStore.saveUser(
            userId = user.usuario,
            email = user.correo,
            isKycVerified = false
        )

        return LoginResult.Success(user)
    }

    /**
     * Registra un nuevo usuario en la base de datos SQL con sus 7 columnas.
     */
    suspend fun register(
        usuario: String,
        numeroTelefonico: String,
        correo: String,
        contrasena: String
    ): RegisterResult {
        val trimmedUser = usuario.trim()
        val trimmedPhone = numeroTelefonico.trim()
        val trimmedEmail = correo.trim()

        if (trimmedUser.isBlank()) {
            return RegisterResult.ValidationError("Ingresa un nombre de usuario")
        }
        if (trimmedPhone.isBlank()) {
            return RegisterResult.ValidationError("Ingresa un número telefónico")
        }
        if (trimmedEmail.isBlank()) {
            return RegisterResult.ValidationError("Ingresa un correo electrónico")
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return RegisterResult.ValidationError("Ingresa un correo electrónico válido")
        }
        if (contrasena.isBlank()) {
            return RegisterResult.ValidationError("Ingresa una contraseña")
        }
        if (contrasena.length < 4) {
            return RegisterResult.ValidationError("La contraseña debe tener al menos 4 caracteres")
        }

        val alreadyExists = userDao.countByEmail(trimmedEmail) > 0
        if (alreadyExists) {
            return RegisterResult.EmailAlreadyExists()
        }

        val newUser = UserEntity(
            correo = trimmedEmail,
            contrasena = contrasena,
            usuario = trimmedUser,
            numeroTelefonico = trimmedPhone,
            cantidadTruequeCoins = 100.0,
            truequesRealizados = 0,
            calificacion = 5.0
        )

        userDao.insertUser(newUser)

        return RegisterResult.Success(newUser)
    }

    /**
     * Obtiene los datos del usuario logueado actualmente a partir de la sesión.
     */
    suspend fun getCurrentUser(): UserEntity? {
        val session = userSessionDataStore.sessionFlow.first()
        val email = session.userEmail ?: return null
        return userDao.getUserByEmail(email)
    }

    suspend fun getUserByEmail(correo: String): UserEntity? {
        return userDao.getUserByEmail(correo)
    }

    fun observeUserByEmail(correo: String): Flow<UserEntity?> {
        return userDao.observeUserByEmail(correo)
    }

    /**
     * Cierra la sesión activa del usuario.
     */
    suspend fun logout() {
        userSessionDataStore.clearSession()
    }
}
