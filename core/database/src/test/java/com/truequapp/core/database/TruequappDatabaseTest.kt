package com.truequapp.core.database

import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.ChatMessageEntity
import com.truequapp.core.database.entity.LedgerEntryEntity
import com.truequapp.core.database.entity.SafeZoneEntity
import com.truequapp.core.database.entity.TradeItemEntity
import com.truequapp.core.database.entity.TradeProposalEntity
import com.truequapp.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Falso en memoria del UserDao para verificar la lógica de acceso a datos sin depender de emuladores.
 */
class InMemoryTestUserDao : UserDao {
    private val usersMap = mutableMapOf<String, UserEntity>()
    private val usersFlow = MutableStateFlow<List<UserEntity>>(emptyList())

    private fun emit() {
        usersFlow.value = usersMap.values.toList()
    }

    override fun getUserById(id: String): Flow<UserEntity?> =
        usersFlow.map { list -> list.find { it.id == id } }

    override suspend fun getUserByIdSync(id: String): UserEntity? = usersMap[id]

    override fun getUserByEmail(email: String): Flow<UserEntity?> =
        usersFlow.map { list -> list.find { it.email == email } }

    override suspend fun getUserByEmailSync(email: String): UserEntity? =
        usersMap.values.find { it.email == email }

    override suspend fun authenticate(email: String, passwordHash: String): UserEntity? =
        usersMap.values.find { it.email == email && it.passwordHash == passwordHash }

    override suspend fun insertUser(user: UserEntity) {
        usersMap[user.id] = user
        emit()
    }

    override suspend fun updateUser(user: UserEntity) {
        usersMap[user.id] = user
        emit()
    }

    override suspend fun updateTruequeCoins(userId: String, availableCoins: Double, escrowCoins: Double) {
        val user = usersMap[userId] ?: return
        usersMap[userId] = user.copy(truequeCoins = availableCoins, escrowCoins = escrowCoins)
        emit()
    }

    override suspend fun getTruequeCoins(userId: String): Double? = usersMap[userId]?.truequeCoins

    override fun getAllUsers(): Flow<List<UserEntity>> = usersFlow

    override suspend fun deleteUser(userId: String) {
        usersMap.remove(userId)
        emit()
    }
}

class TruequappDatabaseTest {

    private lateinit var userDao: InMemoryTestUserDao

    @BeforeEach
    fun setUp() {
        userDao = InMemoryTestUserDao()
    }

    @Test
    @DisplayName("Guarda usuario con email, nombre, contraseña y saldo de TruequeCoins, y permite autenticar")
    fun insertAndAuthenticateUser_withEmailAndPassword() = runBlocking {
        val user = UserEntity(
            id = "usr_001",
            email = "juan.perez@truequapp.com",
            name = "Juan Pérez Coatza",
            passwordHash = "securePassword123",
            authProvider = "EMAIL",
            truequeCoins = 100.0,
            escrowCoins = 0.0,
            city = "Coatzacoalcos"
        )
        userDao.insertUser(user)

        // Autenticación correcta con credenciales
        val authenticatedUser = userDao.authenticate("juan.perez@truequapp.com", "securePassword123")
        assertNotNull(authenticatedUser)
        assertEquals("usr_001", authenticatedUser?.id)
        assertEquals("Juan Pérez Coatza", authenticatedUser?.name)
        assertEquals(100.0, authenticatedUser?.truequeCoins)
        assertEquals("Coatzacoalcos", authenticatedUser?.city)

        // Autenticación fallida con contraseña errónea
        val failedAuth = userDao.authenticate("juan.perez@truequapp.com", "wrongPassword")
        assertNull(failedAuth)
    }

    @Test
    @DisplayName("Guarda usuario con cuenta de Google sin contraseña con KYC nivel 1 verificado")
    fun insertUser_withGoogleAccount() = runBlocking {
        val googleUser = UserEntity(
            id = "google_1029384756",
            email = "mariana.coatza@gmail.com",
            name = "Mariana Sánchez",
            passwordHash = null, // Sin contraseña por ser login con Google
            authProvider = "GOOGLE",
            photoUrl = "https://lh3.googleusercontent.com/photo.jpg",
            isKycVerified = true,
            kycLevel = "TELEFONO_SMS_NIVEL_1",
            truequeCoins = 100.0,
            obfuscatedNeighborhood = "Col. María de la Piedad, Coatzacoalcos",
            city = "Coatzacoalcos"
        )
        userDao.insertUser(googleUser)

        val retrieved = userDao.getUserByEmailSync("mariana.coatza@gmail.com")
        assertNotNull(retrieved)
        assertEquals("GOOGLE", retrieved?.authProvider)
        assertNull(retrieved?.passwordHash)
        assertTrue(retrieved?.isKycVerified == true)
        assertEquals(100.0, retrieved?.truequeCoins)
    }

    @Test
    @DisplayName("Actualiza la cantidad única canónica de TruequeCoins y saldo en custodia Escrow")
    fun updateTruequeCoins_updatesBalanceInDatabase() = runBlocking {
        val user = UserEntity(
            id = "usr_balance_test",
            email = "balance@truequapp.com",
            name = "Balance Test",
            truequeCoins = 100.0,
            escrowCoins = 0.0
        )
        userDao.insertUser(user)

        // Retención en Escrow: 100 TC disponibles -> 60 disponibles + 40 en custodia
        userDao.updateTruequeCoins(userId = "usr_balance_test", availableCoins = 60.0, escrowCoins = 40.0)

        val updated = userDao.getUserByIdSync("usr_balance_test")
        assertEquals(60.0, updated?.truequeCoins)
        assertEquals(40.0, updated?.escrowCoins)
    }

    @Test
    @DisplayName("Valida la estructura de entidad de artículo en Coatzacoalcos y marcadores")
    fun tradeItemEntity_validatesFieldsAndLocation() {
        val item = TradeItemEntity(
            id = "item_01",
            ownerId = "usr_001",
            ownerName = "Juan",
            title = "Bicicleta Malecón",
            description = "Rodada 29",
            categoryId = 7,
            estimatedValueMin = 3000.0,
            estimatedValueMax = 3500.0,
            isBookmarked = true,
            locationNeighborhood = "Malecón, Coatzacoalcos",
            city = "Coatzacoalcos"
        )
        assertEquals("item_01", item.id)
        assertEquals(7, item.categoryId)
        assertTrue(item.isBookmarked)
        assertEquals("Coatzacoalcos", item.city)
    }

    @Test
    @DisplayName("Valida la estructura de asiento contable de partida doble en el libro mayor (RN-04)")
    fun ledgerEntryEntity_recordsDoubleEntryTransactions() {
        val debitEntry = LedgerEntryEntity(
            id = UUID.randomUUID().toString(),
            walletId = "wlt_buyer_01",
            tradeId = "trade_001",
            entryType = "DEBITO",
            amount = 50.0,
            concept = "RETENCION_EN_ESCROW_TRUEQUE",
            counterpartWalletId = "ESCROW_SYSTEM"
        )
        assertEquals(50.0, debitEntry.amount)
        assertEquals("DEBITO", debitEntry.entryType)
        assertEquals("ESCROW_SYSTEM", debitEntry.counterpartWalletId)
    }

    @Test
    @DisplayName("Valida la estructura de Punto Seguro de Coatzacoalcos en la base de datos")
    fun safeZoneEntity_storesCoatzacoalcosSafeZones() {
        val safeZone = SafeZoneEntity(
            id = "sz_coatza_forum",
            name = "Punto Seguro Plaza Forum Coatzacoalcos",
            addressDescription = "Av. Universidad Veracruzana 2601",
            latitude = 18.1415,
            longitude = -94.4852,
            openingHour = 10,
            closingHour = 21,
            hasVideoSurveillance = true,
            isCampusUniversityPartner = false,
            city = "Coatzacoalcos"
        )
        assertEquals("Punto Seguro Plaza Forum Coatzacoalcos", safeZone.name)
        assertEquals(18.1415, safeZone.latitude)
        assertEquals(-94.4852, safeZone.longitude)
        assertEquals("Coatzacoalcos", safeZone.city)
    }
}
