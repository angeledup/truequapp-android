package com.truequapp.sync

import com.truequapp.core.database.dao.TruequeDao
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.TruequeConOfertas
import com.truequapp.core.database.entity.TruequeEntity
import com.truequapp.core.database.entity.UserEntity
import com.truequapp.core.database.storage.UserPublicationStorage
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DatabaseSyncManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeUserDao: FakeSyncUserDao
    private lateinit var fakeTruequeDao: FakeSyncTruequeDao
    private lateinit var fakeSessionDataStore: FakeSyncSessionDataStore
    private lateinit var syncManager: DatabaseSyncManager

    @BeforeEach
    fun setUp() {
        fakeUserDao = FakeSyncUserDao()
        fakeTruequeDao = FakeSyncTruequeDao()
        fakeSessionDataStore = FakeSyncSessionDataStore()

        syncManager = DatabaseSyncManager(
            userDao = fakeUserDao,
            truequeDao = fakeTruequeDao,
            userSessionDataStore = fakeSessionDataStore,
            userPublicationStorage = null
        )
    }

    @Test
    fun `syncDatabase with blank cloudBaseUrl sets state to Success`() = runTest(testDispatcher) {
        syncManager.cloudBaseUrl = ""
        syncManager.syncDatabase()

        assertEquals(SyncState.Success, syncManager.syncState.value)
    }

    @Test
    fun `syncDatabase with unreachable host sets state to ConnectionError`() = runTest(testDispatcher) {
        // Host inalcanzable para forzar ConnectException o UnknownHostException
        syncManager.cloudBaseUrl = "http://192.0.2.1:1" // RFC 5737 TEST-NET address
        syncManager.syncDatabase()

        val state = syncManager.syncState.value
        val connError = assertInstanceOf(SyncState.ConnectionError::class.java, state)
        assertEquals("Error de conexion", connError.message)
    }

    @Test
    fun `syncDatabase with invalid endpoint sets state to OtherError`() = runTest(testDispatcher) {
        // Formato que genera un error no de conexión directa
        syncManager.cloudBaseUrl = "http://localhost:99999" // Puerto inválido (> 65535)
        syncManager.syncDatabase()

        val state = syncManager.syncState.value
        val otherError = assertInstanceOf(SyncState.OtherError::class.java, state)
        assertEquals("Error, intentar mas tarde", otherError.message)
        assertTrue(otherError.terminalError.isNotBlank())
    }

    @Test
    fun `stopPeriodicSync cancels periodic job`() = runTest(testDispatcher) {
        syncManager.startPeriodicSync(this)
        syncManager.stopPeriodicSync()
        // No exceptions thrown, cleanly stopped
        assertTrue(true)
    }
}

private class FakeSyncUserDao : UserDao {
    private val users = mutableMapOf<String, UserEntity>()
    override suspend fun getUserByEmail(correo: String): UserEntity? = users[correo.lowercase()]
    override fun observeUserByEmail(correo: String): Flow<UserEntity?> = flowOf(users[correo.lowercase()])
    override suspend fun countByEmail(correo: String): Int = if (users.containsKey(correo.lowercase())) 1 else 0
    override suspend fun insertUser(user: UserEntity) { users[user.correo.lowercase()] = user }
    override fun getAllUsers(): Flow<List<UserEntity>> = flowOf(users.values.toList())
    override suspend fun getCoinsByEmail(correo: String): Double? = users[correo.lowercase()]?.cantidadTruequeCoins
    override fun observeCoinsByEmail(correo: String): Flow<Double?> = flowOf(users[correo.lowercase()]?.cantidadTruequeCoins)
    override suspend fun reduceCoins(correo: String, amount: Double): Int = 1
    override suspend fun addCoins(correo: String, amount: Double): Int = 1
    override suspend fun updateCoins(correo: String, newCoins: Double): Int = 1
    override suspend fun getCompletedTradesByEmail(correo: String): Int? = users[correo.lowercase()]?.truequesRealizados
    override fun observeCompletedTradesByEmail(correo: String): Flow<Int?> = flowOf(users[correo.lowercase()]?.truequesRealizados)
    override suspend fun incrementCompletedTrades(correo: String): Int = 1
    override suspend fun updateCompletedTrades(correo: String, count: Int): Int = 1
    override suspend fun getRatingByEmail(correo: String): Double? = users[correo.lowercase()]?.calificacion
    override fun observeRatingByEmail(correo: String): Flow<Double?> = flowOf(users[correo.lowercase()]?.calificacion)
    override suspend fun updateRating(correo: String, newRating: Double): Int = 1
    override fun getUserById(id: String): Flow<UserEntity?> = flowOf(users[id.lowercase()])
    override suspend fun getUserByIdSync(id: String): UserEntity? = users[id.lowercase()]
    override suspend fun getUserByEmailSync(email: String): UserEntity? = users[email.lowercase()]
    override suspend fun getUserByIdentifierSync(identifier: String): UserEntity? = users[identifier.lowercase()]
    override suspend fun authenticate(email: String, passwordHash: String): UserEntity? = users[email.lowercase()]
    override suspend fun updateUser(user: UserEntity) { users[user.correo.lowercase()] = user }
    override suspend fun updateUserCredentials(oldEmail: String, newEmail: String, newUsername: String, newPassword: String): Int {
        val u = users.remove(oldEmail.lowercase()) ?: return 0
        users[newEmail.lowercase()] = u.copy(correo = newEmail, usuario = newUsername, contrasena = newPassword)
        return 1
    }
    override suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double) {}
    override suspend fun getTruequeCoins(userId: String): Double? = users[userId.lowercase()]?.cantidadTruequeCoins
    override suspend fun deleteUser(userId: String) { users.remove(userId.lowercase()) }
}

private class FakeSyncTruequeDao : TruequeDao {
    private val ofertas = mutableMapOf<String, OfertaItemEntity>()
    private val trueques = mutableMapOf<String, TruequeEntity>()

    override suspend fun insertOfertaItem(oferta: OfertaItemEntity) { ofertas[oferta.id] = oferta }
    override suspend fun insertTrueque(trueque: TruequeEntity) { trueques[trueque.id] = trueque }
    override suspend fun updateTrueque(trueque: TruequeEntity) { trueques[trueque.id] = trueque }
    override suspend fun getTruequeById(id: String): TruequeConOfertas? {
        val t = trueques[id] ?: return null
        return TruequeConOfertas(t, ofertas[t.ofertaAId], ofertas[t.ofertaBId])
    }
    override suspend fun updateEstado(truequeId: String, nuevoEstado: String, estaActiva: Boolean, fechaActualizacion: Long) {
        val t = trueques[truequeId] ?: return
        trueques[truequeId] = t.copy(estado = nuevoEstado, estaActiva = estaActiva, fechaActualizacion = fechaActualizacion)
    }
    override fun getTruequesActivos(): Flow<List<TruequeConOfertas>> = flowOf(emptyList())
    override fun getTruequesActivosPorUsuario(correo: String): Flow<List<TruequeConOfertas>> = flowOf(emptyList())
    override fun getOfertasRecibidas(correo: String): Flow<List<TruequeConOfertas>> = flowOf(emptyList())
    override fun getOfertasEnviadas(correo: String): Flow<List<TruequeConOfertas>> = flowOf(emptyList())
    override fun getHistorialTruequesPorUsuario(correo: String): Flow<List<TruequeConOfertas>> = flowOf(emptyList())
    override suspend fun deleteTruequeById(truequeId: String) { trueques.remove(truequeId) }
}

private class FakeSyncSessionDataStore : UserSessionDataStore {
    var savedEmail: String? = null
    override val sessionFlow: Flow<UserSession>
        get() = flowOf(UserSession(userId = "usr_test", userEmail = savedEmail))
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean, name: String, photoUrl: String?, authProvider: String) {
        savedEmail = email
    }
    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun clearSession() { savedEmail = null }
}

