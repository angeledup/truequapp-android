package com.truequapp.auth.data

import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.datastore.DatabaseValidationModeManager
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.DatabaseValidationMode
import com.truequapp.core.datastore.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LoginResult {
    data class Success(val user: UserEntity) : LoginResult
    data class UserNotFound(
        val message: String = "No hay una cuenta vinculada al correo",
        val email: String = ""
    ) : LoginResult
    data class InvalidPassword(val message: String = "Contraseña incorrecta") : LoginResult
    data class ValidationError(val message: String) : LoginResult
}

sealed interface RegisterResult {
    data class Success(val user: UserEntity) : RegisterResult
    data class EmailAlreadyExists(val message: String = "Ya existe una cuenta registrada con este correo") : RegisterResult
    data class ValidationError(val message: String) : RegisterResult
}

sealed interface UpdateProfileResult {
    data class Success(val updatedUser: UserEntity) : UpdateProfileResult
    data class EmailAlreadyExists(val message: String = "El correo ya se encuentra registrado por otra cuenta") : UpdateProfileResult
    data class ValidationError(val message: String) : UpdateProfileResult
    data class Error(val message: String) : UpdateProfileResult
}

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val userSessionDataStore: UserSessionDataStore,
    private val validationModeManager: DatabaseValidationModeManager? = null
) {

    var cloudBaseUrl: String = "https://truequapp-pi.vercel.app"

    val isOnlineMode: Boolean
        get() = (validationModeManager?.getValidationMode() ?: DatabaseValidationMode.DB_ONLINE) == DatabaseValidationMode.DB_ONLINE

    val currentSessionFlow: Flow<UserSession> get() = userSessionDataStore.sessionFlow

    /**
     * Valida credenciales contra la base de datos en la nube (Turso) en primer lugar.
     * Si no se encuentra en la base de datos, retorna UserNotFound para que la interfaz
     * notifique al usuario y le permita continuar con el protocolo de registro.
     */
    suspend fun login(correo: String, contrasena: String): LoginResult {
        val trimmedEmail = correo.trim()

        if (trimmedEmail.isBlank()) {
            return LoginResult.ValidationError("Por favor ingresa tu correo electrónico")
        }
        if (contrasena.isBlank()) {
            return LoginResult.ValidationError("Por favor ingresa tu contraseña")
        }

        // 1. Validar primero contra la base de datos en la nube (Turso vía Vercel) si estamos en modo db-Online
        var cloudAttempted = false
        var cloudUserNotFound = false
        var cloudInvalidPassword = false
        var cloudUserEntity: UserEntity? = null

        if (isOnlineMode && cloudBaseUrl.isNotBlank()) {
            try {
                val url = java.net.URL("${cloudBaseUrl.trimEnd('/')}/api/usuarios/login")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3500
                conn.readTimeout = 3500
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                val json = org.json.JSONObject().apply {
                    put("identifier", trimmedEmail)
                    put("password", contrasena)
                }
                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                cloudAttempted = true
                val responseCode = conn.responseCode

                if (responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val respObj = org.json.JSONObject(responseText)
                    if (respObj.optBoolean("success")) {
                        val uObj = respObj.optJSONObject("user")
                        if (uObj != null) {
                            cloudUserEntity = UserEntity(
                                correo = uObj.optString("email", trimmedEmail),
                                contrasena = contrasena,
                                usuario = uObj.optString("name", trimmedEmail.substringBefore("@")),
                                numeroTelefonico = uObj.optString("phoneNumber", "+520000000000"),
                                cantidadTruequeCoins = uObj.optDouble("truequeCoins", 100.0),
                                truequesRealizados = uObj.optInt("completedTrades", 0),
                                calificacion = uObj.optDouble("bayesianRating", 5.0)
                            )
                        }
                    }
                } else if (responseCode == 404) {
                    cloudUserNotFound = true
                } else if (responseCode == 401) {
                    cloudInvalidPassword = true
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Silencioso ante falta de red o error de conexión
            }
        }

        if (cloudAttempted) {
            if (cloudUserNotFound) {
                // Si la nube confirma que no existe el usuario, purgar cualquier dato huérfano local en Room
                val localGhost = userDao.getUserByEmail(trimmedEmail)
                if (localGhost != null) {
                    try {
                        userDao.deleteUser(localGhost.id)
                    } catch (_: Exception) {}
                }
                return LoginResult.UserNotFound(
                    message = "No hay una cuenta vinculada a este correo en la base de datos",
                    email = trimmedEmail
                )
            }

            if (cloudInvalidPassword) {
                return LoginResult.InvalidPassword()
            }

            if (cloudUserEntity != null) {
                // Sincronizar en la base de datos local SQLite Room
                userDao.insertUser(cloudUserEntity)
                userSessionDataStore.saveUser(
                    userId = cloudUserEntity.usuario,
                    email = cloudUserEntity.correo,
                    isKycVerified = false
                )
                return LoginResult.Success(cloudUserEntity)
            }
        }

        // 2. Si no hubo conectividad a la nube o no se pudo verificar online, consultar la base de datos local SQLite Room
        val localUser = userDao.getUserByEmail(trimmedEmail)
        if (localUser == null) {
            return LoginResult.UserNotFound(
                message = "No hay una cuenta vinculada al correo",
                email = trimmedEmail
            )
        }

        if (localUser.contrasena != contrasena) {
            return LoginResult.InvalidPassword()
        }

        // Al autenticar con éxito, se persiste la sesión del usuario
        userSessionDataStore.saveUser(
            userId = localUser.usuario,
            email = localUser.correo,
            isKycVerified = false
        )

        return LoginResult.Success(localUser)
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

        // 1. Validar existencia previa directamente con db-Online (Turso) si estamos en modo online
        if (isOnlineMode && cloudBaseUrl.isNotBlank()) {
            try {
                val url = java.net.URL("${cloudBaseUrl.trimEnd('/')}/api/usuarios/register")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 4000
                conn.readTimeout = 4000
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                val json = org.json.JSONObject().apply {
                    put("id", "usr_${java.util.UUID.randomUUID().toString().take(8)}")
                    put("email", trimmedEmail)
                    put("name", trimmedUser)
                    put("passwordHash", contrasena)
                    put("phoneNumber", trimmedPhone)
                    put("truequeCoins", 100.0)
                    put("bayesianRating", 5.0)
                    put("ratingsCount", 0)
                    put("completedTrades", 0)
                }
                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                conn.disconnect()
                if (code == 409) {
                    return RegisterResult.EmailAlreadyExists("Ya existe una cuenta registrada con este correo en la base de datos")
                }
            } catch (_: Exception) {
                // Silencioso ante falta de red para permitir modo offline
            }
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
     * Modifica el nombre de usuario, contraseña y correo registrado (Ajustes de cuenta).
     * Valida unicidad contra la base de datos en la nube (Turso) y actualiza Room y DataStore.
     */
    suspend fun updateProfile(
        currentEmail: String,
        newUsername: String,
        newPassword: String,
        newEmail: String
    ): UpdateProfileResult {
        val trimmedCurrEmail = currentEmail.trim()
        val trimmedUser = newUsername.trim()
        val trimmedEmail = newEmail.trim()

        if (trimmedUser.isBlank()) {
            return UpdateProfileResult.ValidationError("Ingresa un nombre de usuario")
        }
        if (trimmedEmail.isBlank()) {
            return UpdateProfileResult.ValidationError("Ingresa un correo electrónico")
        }
        if (!trimmedEmail.contains("@") || !trimmedEmail.contains(".")) {
            return UpdateProfileResult.ValidationError("Ingresa un correo electrónico válido")
        }
        if (newPassword.isBlank()) {
            return UpdateProfileResult.ValidationError("Ingresa una contraseña")
        }
        if (newPassword.length < 4) {
            return UpdateProfileResult.ValidationError("La contraseña debe tener al menos 4 caracteres")
        }

        // 1. Validar y actualizar en db-Online (Turso) si estamos en modo online
        if (isOnlineMode && cloudBaseUrl.isNotBlank()) {
            try {
                val url = java.net.URL("${cloudBaseUrl.trimEnd('/')}/api/usuarios/update")
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 4500
                conn.readTimeout = 4500
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true

                val json = org.json.JSONObject().apply {
                    put("currentEmail", trimmedCurrEmail)
                    put("newEmail", trimmedEmail)
                    put("newUsername", trimmedUser)
                    put("newPassword", newPassword)
                }
                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = conn.responseCode
                if (responseCode == 409) {
                    conn.disconnect()
                    return UpdateProfileResult.EmailAlreadyExists("El correo ya se encuentra registrado por otra cuenta en la base de datos")
                } else if (responseCode !in 200..299) {
                    val err = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    conn.disconnect()
                    return UpdateProfileResult.Error("Error al actualizar en la nube: $err")
                }
                conn.disconnect()
            } catch (_: Throwable) {
                // Modo offline / fallo de red
            }
        }

        // 2. Validación local si el correo cambia
        if (!trimmedCurrEmail.equals(trimmedEmail, ignoreCase = true)) {
            val existsLocal = userDao.countByEmail(trimmedEmail) > 0
            if (existsLocal) {
                return UpdateProfileResult.EmailAlreadyExists()
            }
        }

        // 3. Persistir en la base de datos local (Room)
        val existingUser = userDao.getUserByEmail(trimmedCurrEmail)
        val rowsUpdated = userDao.updateUserCredentials(
            oldEmail = trimmedCurrEmail,
            newEmail = trimmedEmail,
            newUsername = trimmedUser,
            newPassword = newPassword
        )

        val updatedUser = if (rowsUpdated > 0) {
            userDao.getUserByEmail(trimmedEmail) ?: existingUser?.copy(
                correo = trimmedEmail,
                usuario = trimmedUser,
                contrasena = newPassword
            )
        } else {
            val newUser = existingUser?.copy(
                correo = trimmedEmail,
                usuario = trimmedUser,
                contrasena = newPassword
            ) ?: UserEntity(
                correo = trimmedEmail,
                contrasena = newPassword,
                usuario = trimmedUser,
                numeroTelefonico = ""
            )
            userDao.insertUser(newUser)
            newUser
        }

        // 4. Actualizar la sesión activa en DataStore para mantener sincronía
        if (updatedUser != null) {
            userSessionDataStore.saveUser(
                userId = updatedUser.usuario,
                email = updatedUser.correo,
                isKycVerified = true,
                name = updatedUser.usuario,
                photoUrl = null,
                authProvider = "local"
            )
            return UpdateProfileResult.Success(updatedUser)
        }

        return UpdateProfileResult.Error("No se pudo actualizar el perfil en la base de datos local")
    }

    /**
     * Cierra la sesión activa del usuario.
     */
    suspend fun logout() {
        userSessionDataStore.clearSession()
    }
}
