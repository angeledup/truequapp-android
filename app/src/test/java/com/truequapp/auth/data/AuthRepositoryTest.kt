package com.truequapp.auth.data

import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthRepositoryTest {

    private lateinit var fakeUserDao: FakeUserDao
    private lateinit var fakeSessionDataStore: FakeSessionDataStore
    private lateinit var authRepository: AuthRepository

    @BeforeEach
    fun setUp() {
        fakeUserDao = FakeUserDao()
        fakeSessionDataStore = FakeSessionDataStore()
        authRepository = AuthRepository(fakeUserDao, fakeSessionDataStore)
    }

    @Test
    fun `login returns UserNotFound when email is not registered in database`() = runTest {
        val result = authRepository.login("no_existe@correo.com", "password123")

        val notFound = assertInstanceOf(LoginResult.UserNotFound::class.java, result)
        assertEquals("No hay una cuenta vinculada al correo", notFound.message)
    }

    @Test
    fun `login returns InvalidPassword when email exists but password is incorrect`() = runTest {
        // Pre-insertar usuario
        fakeUserDao.insertUser(
            UserEntity(
                correo = "angel@correo.com",
                contrasena = "correctPassword",
                usuario = "angel",
                numeroTelefonico = "+525511223344"
            )
        )

        val result = authRepository.login("angel@correo.com", "wrongPassword")

        val invalidPass = assertInstanceOf(LoginResult.InvalidPassword::class.java, result)
        assertEquals("Contraseña incorrecta", invalidPass.message)
    }

    @Test
    fun `login succeeds when email and password match database record`() = runTest {
        val existingUser = UserEntity(
            correo = "angel@correo.com",
            contrasena = "secret123",
            usuario = "angel_dev",
            numeroTelefonico = "+525599887766"
        )
        fakeUserDao.insertUser(existingUser)

        val result = authRepository.login("angel@correo.com", "secret123")

        val success = assertInstanceOf(LoginResult.Success::class.java, result)
        assertEquals("angel@correo.com", success.user.correo)
        assertEquals("angel_dev", success.user.usuario)
        assertEquals("angel_dev", fakeSessionDataStore.savedUserId)
        assertEquals("angel@correo.com", fakeSessionDataStore.savedEmail)
    }

    @Test
    fun `register persists user with all 7 columns in database`() = runTest {
        val result = authRepository.register(
            usuario = "truequero_cdmx",
            numeroTelefonico = "+525544332211",
            correo = "nuevo@correo.com",
            contrasena = "miPassword123"
        )

        val success = assertInstanceOf(RegisterResult.Success::class.java, result)
        assertEquals("nuevo@correo.com", success.user.correo)
        assertEquals("miPassword123", success.user.contrasena)
        assertEquals("truequero_cdmx", success.user.usuario)
        assertEquals("+525544332211", success.user.numeroTelefonico)
        assertEquals(100.0, success.user.cantidadTruequeCoins)
        assertEquals(0, success.user.truequesRealizados)
        assertEquals(5.0, success.user.calificacion)

        // Verificar que quedó en la base de datos
        val userInDb = fakeUserDao.getUserByEmail("nuevo@correo.com")
        assertTrue(userInDb != null)
        assertEquals("truequero_cdmx", userInDb?.usuario)
    }

    @Test
    fun `register rejects duplicate email if already registered in database`() = runTest {
        fakeUserDao.insertUser(
            UserEntity(
                correo = "repetido@correo.com",
                contrasena = "pass1",
                usuario = "usuario1",
                numeroTelefonico = "12345678"
            )
        )

        val result = authRepository.register(
            usuario = "usuario2",
            numeroTelefonico = "87654321",
            correo = "repetido@correo.com",
            contrasena = "pass2"
        )

        assertInstanceOf(RegisterResult.EmailAlreadyExists::class.java, result)
    }

    @Test
    fun `getCurrentUser returns logged in user details from database`() = runTest {
        val user = UserEntity(
            correo = "sesion@correo.com",
            contrasena = "pass123",
            usuario = "usuario_sesion",
            numeroTelefonico = "5511223344",
            cantidadTruequeCoins = 150.0,
            truequesRealizados = 3,
            calificacion = 4.8
        )
        fakeUserDao.insertUser(user)
        fakeSessionDataStore.saveUser("usuario_sesion", "sesion@correo.com", false)

        val current = authRepository.getCurrentUser()
        assertEquals("sesion@correo.com", current?.correo)
        assertEquals("usuario_sesion", current?.usuario)
        assertEquals(150.0, current?.cantidadTruequeCoins)
        assertEquals(3, current?.truequesRealizados)
        assertEquals(4.8, current?.calificacion)
    }

    @Test
    fun `logout clears session DataStore`() = runTest {
        fakeSessionDataStore.saveUser("usr1", "usr1@correo.com", false)
        authRepository.logout()

        assertNull(fakeSessionDataStore.savedUserId)
        assertNull(fakeSessionDataStore.savedEmail)
    }
}

private class FakeUserDao : UserDao {
    private val users = mutableMapOf<String, UserEntity>()

    override suspend fun getUserByEmail(correo: String): UserEntity? {
        return users[correo.lowercase()]
    }

    override fun observeUserByEmail(correo: String): Flow<UserEntity?> {
        return flowOf(users[correo.lowercase()])
    }

    override suspend fun countByEmail(correo: String): Int {
        return if (users.containsKey(correo.lowercase())) 1 else 0
    }

    override suspend fun insertUser(user: UserEntity) {
        users[user.correo.lowercase()] = user
    }

    override fun getAllUsers(): Flow<List<UserEntity>> {
        return flowOf(users.values.toList())
    }
}

private class FakeSessionDataStore : UserSessionDataStore {
    var savedUserId: String? = null
    var savedEmail: String? = null

    override val sessionFlow: Flow<UserSession>
        get() = flowOf(UserSession(userId = savedUserId, userEmail = savedEmail))

    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}

    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean) {
        savedUserId = userId
        savedEmail = email
    }

    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun clearSession() {
        savedUserId = null
        savedEmail = null
    }
}
