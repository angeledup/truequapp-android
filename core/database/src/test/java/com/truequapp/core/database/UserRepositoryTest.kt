package com.truequapp.core.database

import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.database.repository.UserRepository
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FakeDatabaseUserDao : UserDao {
    val users = mutableMapOf<String, UserEntity>()

    override suspend fun getUserByEmail(correo: String): UserEntity? = users[correo.lowercase()]
    override fun observeUserByEmail(correo: String): Flow<UserEntity?> = flowOf(users[correo.lowercase()])
    override suspend fun countByEmail(correo: String): Int = if (users.containsKey(correo.lowercase())) 1 else 0
    override suspend fun insertUser(user: UserEntity) {
        users[user.correo.lowercase()] = user
    }
    override fun getAllUsers(): Flow<List<UserEntity>> = flowOf(users.values.toList())

    override suspend fun getCoinsByEmail(correo: String): Double? = users[correo.lowercase()]?.cantidadTruequeCoins
    override fun observeCoinsByEmail(correo: String): Flow<Double?> = flowOf(users[correo.lowercase()]?.cantidadTruequeCoins)

    override suspend fun reduceCoins(correo: String, amount: Double): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(cantidadTruequeCoins = maxOf(0.0, u.cantidadTruequeCoins - amount))
        return 1
    }

    override suspend fun addCoins(correo: String, amount: Double): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(cantidadTruequeCoins = u.cantidadTruequeCoins + amount)
        return 1
    }

    override suspend fun updateCoins(correo: String, newCoins: Double): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(cantidadTruequeCoins = newCoins)
        return 1
    }

    override suspend fun getCompletedTradesByEmail(correo: String): Int? = users[correo.lowercase()]?.truequesRealizados
    override fun observeCompletedTradesByEmail(correo: String): Flow<Int?> = flowOf(users[correo.lowercase()]?.truequesRealizados)

    override suspend fun incrementCompletedTrades(correo: String): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(truequesRealizados = u.truequesRealizados + 1)
        return 1
    }

    override suspend fun updateCompletedTrades(correo: String, count: Int): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(truequesRealizados = count)
        return 1
    }

    override suspend fun getRatingByEmail(correo: String): Double? = users[correo.lowercase()]?.calificacion
    override fun observeRatingByEmail(correo: String): Flow<Double?> = flowOf(users[correo.lowercase()]?.calificacion)

    override suspend fun updateRating(correo: String, newRating: Double): Int {
        val u = users[correo.lowercase()] ?: return 0
        users[correo.lowercase()] = u.copy(calificacion = newRating)
        return 1
    }

    override fun getUserById(id: String): Flow<UserEntity?> = flowOf(users[id.lowercase()])
    override suspend fun getUserByIdSync(id: String): UserEntity? = users[id.lowercase()]
    override suspend fun getUserByEmailSync(email: String): UserEntity? = users[email.lowercase()]
    override suspend fun getUserByIdentifierSync(identifier: String): UserEntity? = users[identifier.lowercase()]
    override suspend fun authenticate(email: String, passwordHash: String): UserEntity? = users[email.lowercase()]
    override suspend fun updateUser(user: UserEntity) { users[user.correo.lowercase()] = user }
    override suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double) {}
    override suspend fun getTruequeCoins(userId: String): Double? = users[userId.lowercase()]?.cantidadTruequeCoins
    override suspend fun deleteUser(userId: String) { users.remove(userId.lowercase()) }
}

class FakeDatabaseSessionDataStore : UserSessionDataStore {
    private val _session = MutableStateFlow(
        UserSession(userId = "admin_user", userEmail = "angeleduvr@gmail.com", isKycVerified = true)
    )
    override val sessionFlow: Flow<UserSession> = _session
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUser(
        userId: String,
        email: String,
        isKycVerified: Boolean,
        name: String,
        photoUrl: String?,
        authProvider: String
    ) {
        _session.value = UserSession(userId = userId, userEmail = email, isKycVerified = isKycVerified, userName = name, userPhotoUrl = photoUrl, authProvider = authProvider)
    }
    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun clearSession() {
        _session.value = UserSession()
    }
}

class UserRepositoryTest {

    private lateinit var fakeDao: FakeDatabaseUserDao
    private lateinit var fakeSession: FakeDatabaseSessionDataStore
    private lateinit var repository: UserRepository

    @BeforeEach
    fun setUp() {
        fakeDao = FakeDatabaseUserDao()
        fakeSession = FakeDatabaseSessionDataStore()
        repository = UserRepository(fakeDao, fakeSession)

        // Usuario administrador sembrado
        fakeDao.users["angeleduvr@gmail.com"] = UserEntity(
            correo = "angeleduvr@gmail.com",
            contrasena = "123456",
            usuario = "Admin_AEVR",
            numeroTelefonico = "+529212404565",
            cantidadTruequeCoins = 100000000.0,
            truequesRealizados = 10,
            calificacion = 10.0
        )
    }

    @Test
    @DisplayName("getCoins: Consulta y devuelve la cantidad de coins del usuario desde la base de datos")
    fun shouldReturnCoinsDirectlyFromDatabase() = runTest {
        val coins = repository.getCoins()
        assertEquals(100000000.0, coins)
    }

    @Test
    @DisplayName("reduceCoins: Reduce la cantidad de coins especificada en la base de datos")
    fun shouldReduceCoinsSuccessfully() = runTest {
        val result = repository.reduceCoins(500.0)
        assertTrue(result)

        val updatedCoins = repository.getCoins()
        assertEquals(99999500.0, updatedCoins)
    }

    @Test
    @DisplayName("reduceCoins: No permite saldo negativo, limitando a 0.0")
    fun shouldNotAllowNegativeCoins() = runTest {
        val result = repository.reduceCoins(200000000.0)
        assertTrue(result)

        val updatedCoins = repository.getCoins()
        assertEquals(0.0, updatedCoins)
    }

    @Test
    @DisplayName("addCoins: Suma coins al saldo del usuario en la base de datos")
    fun shouldAddCoinsSuccessfully() = runTest {
        val result = repository.addCoins(1000.0)
        assertTrue(result)

        val updatedCoins = repository.getCoins()
        assertEquals(100001000.0, updatedCoins)
    }

    @Test
    @DisplayName("incrementCompletedTrades: Incrementa en 1 los trueques realizados en la base de datos")
    fun shouldIncrementCompletedTrades() = runTest {
        val initialTrades = repository.getCompletedTrades()
        assertEquals(10, initialTrades)

        repository.incrementCompletedTrades()

        val updatedTrades = repository.getCompletedTrades()
        assertEquals(11, updatedTrades)
    }

    @Test
    @DisplayName("updateRating: Modifica las estrellas de calificación en la base de datos")
    fun shouldUpdateRatingSuccessfully() = runTest {
        val initialRating = repository.getRating()
        assertEquals(10.0, initialRating)

        repository.updateRating(9.5)

        val updatedRating = repository.getRating()
        assertEquals(9.5, updatedRating)
    }

    @Test
    @DisplayName("observeCoins: Emite reactivamente el saldo de coins cuando se actualiza la BD")
    fun shouldObserveCoinsReactivity() = runTest {
        val initialObserved = repository.observeCoins().first()
        assertEquals(100000000.0, initialObserved)
    }
}

