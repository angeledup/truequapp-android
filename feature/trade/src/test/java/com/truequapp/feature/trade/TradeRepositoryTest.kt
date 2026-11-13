package com.truequapp.feature.trade

import com.truequapp.core.database.dao.TruequeDao
import com.truequapp.core.database.entity.OfertaItemEntity
import com.truequapp.core.database.entity.TruequeConOfertas
import com.truequapp.core.database.entity.TruequeEntity
import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import com.truequapp.feature.trade.data.TradeRepository
import com.truequapp.feature.trade.data.toTradeProposal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FakeTruequeDao : TruequeDao {
    val ofertasMap = mutableMapOf<String, OfertaItemEntity>()
    val truequesMap = mutableMapOf<String, TruequeEntity>()

    override suspend fun insertOfertaItem(oferta: OfertaItemEntity) {
        ofertasMap[oferta.id] = oferta
    }

    override suspend fun insertTrueque(trueque: TruequeEntity) {
        truequesMap[trueque.id] = trueque
    }

    override suspend fun updateTrueque(trueque: TruequeEntity) {
        truequesMap[trueque.id] = trueque
    }

    override suspend fun updateEstado(
        truequeId: String,
        nuevoEstado: String,
        estaActiva: Boolean,
        timestamp: Long
    ) {
        val existing = truequesMap[truequeId]
        if (existing != null) {
            truequesMap[truequeId] = existing.copy(
                estado = nuevoEstado,
                estaActiva = estaActiva,
                fechaActualizacion = timestamp
            )
        }
    }

    override suspend fun getTruequeById(truequeId: String): TruequeConOfertas? {
        val trueque = truequesMap[truequeId] ?: return null
        val ofertaA = trueque.ofertaAId?.let { ofertasMap[it] }
        val ofertaB = trueque.ofertaBId?.let { ofertasMap[it] }
        return TruequeConOfertas(trueque = trueque, ofertaA = ofertaA, ofertaB = ofertaB)
    }

    override fun getTruequesActivos(): Flow<List<TruequeConOfertas>> {
        val list = truequesMap.values.filter { it.estaActiva }.map { t ->
            TruequeConOfertas(t, t.ofertaAId?.let { ofertasMap[it] }, t.ofertaBId?.let { ofertasMap[it] })
        }
        return flowOf(list)
    }

    override fun getTruequesActivosPorUsuario(correo: String): Flow<List<TruequeConOfertas>> {
        val list = truequesMap.values.filter {
            (it.usuarioACorreo == correo || it.usuarioBCorreo == correo) && it.estaActiva
        }.map { t ->
            TruequeConOfertas(t, t.ofertaAId?.let { ofertasMap[it] }, t.ofertaBId?.let { ofertasMap[it] })
        }
        return flowOf(list)
    }

    override fun getHistorialTruequesPorUsuario(correo: String): Flow<List<TruequeConOfertas>> {
        val list = truequesMap.values.filter {
            it.usuarioACorreo == correo || it.usuarioBCorreo == correo
        }.map { t ->
            TruequeConOfertas(t, t.ofertaAId?.let { ofertasMap[it] }, t.ofertaBId?.let { ofertasMap[it] })
        }
        return flowOf(list)
    }

    override fun getOfertasRecibidas(correo: String): Flow<List<TruequeConOfertas>> {
        val list = truequesMap.values.filter {
            it.usuarioBCorreo == correo && it.estaActiva
        }.map { t ->
            TruequeConOfertas(t, t.ofertaAId?.let { ofertasMap[it] }, t.ofertaBId?.let { ofertasMap[it] })
        }
        return flowOf(list)
    }

    override fun getOfertasEnviadas(correo: String): Flow<List<TruequeConOfertas>> {
        val list = truequesMap.values.filter {
            it.usuarioACorreo == correo && it.estaActiva
        }.map { t ->
            TruequeConOfertas(t, t.ofertaAId?.let { ofertasMap[it] }, t.ofertaBId?.let { ofertasMap[it] })
        }
        return flowOf(list)
    }

    override suspend fun deleteTruequeById(truequeId: String) {
        truequesMap.remove(truequeId)
    }
}

@DisplayName("Pruebas Unitarias de TradeRepository y Persistencia del Módulo 3")
class TradeRepositoryTest {

    private lateinit var fakeDao: FakeTruequeDao
    private lateinit var repository: TradeRepository

    @BeforeEach
    fun setUp() {
        fakeDao = FakeTruequeDao()
        repository = TradeRepository(fakeDao)
    }

    @Test
    @DisplayName("saveProposal persiste Oferta A, Oferta B y Trueque con estaActiva = true según el boceto")
    fun saveProposal_persistsOffersAndTruequeConEstaActivaTrue() = runTest {
        val proposal = TradeProposal(
            id = "prop_repo_01",
            requesterUserId = "userA@truequapp.com",
            receiverUserId = "userB@truequapp.com",
            offeredItemIds = listOf("item_guitarra", "item_ampli"),
            targetItemId = "item_teclado",
            compensatoryTruequeCoins = 200.0,
            modality = TradeModality.HIBRIDO_CON_CREDITOS,
            status = TradeStatus.PENDIENTE
        )

        val result = repository.saveProposal(
            proposal = proposal,
            userACorreo = "userA@truequapp.com",
            userBCorreo = "userB@truequapp.com"
        )

        // Verificar que el agregador y las tablas contienen los datos
        assertNotNull(result)
        assertEquals("prop_repo_01", result.trueque.id)
        assertTrue(result.estaActiva, "La variable 'estaActiva' en una propuesta PENDIENTE debe ser TRUE")
        assertEquals(true, fakeDao.truequesMap["prop_repo_01"]?.estaActiva)

        // Verificar Oferta A
        val ofertaA = fakeDao.ofertasMap["of_a_prop_repo_01"]
        assertNotNull(ofertaA)
        assertEquals("userA@truequapp.com", ofertaA?.usuarioCorreo)
        assertEquals("item_guitarra,item_ampli", ofertaA?.articulosIds)
        assertEquals(200.0, ofertaA?.truequeCoins)

        // Verificar Oferta B
        val ofertaB = fakeDao.ofertasMap["of_b_prop_repo_01"]
        assertNotNull(ofertaB)
        assertEquals("userB@truequapp.com", ofertaB?.usuarioCorreo)
        assertEquals("item_teclado", ofertaB?.articulosIds)
    }

    @Test
    @DisplayName("Aceptar propuesta (RESERVADO) mantiene estaActiva = true")
    fun acceptProposal_maintainsEstaActivaTrue() = runTest {
        val proposal = TradeProposal(
            id = "prop_repo_02",
            requesterUserId = "userA@truequapp.com",
            receiverUserId = "userB@truequapp.com",
            offeredItemIds = listOf("item_1"),
            targetItemId = "item_2",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.PENDIENTE
        )
        repository.saveProposal(proposal, "userA@truequapp.com", "userB@truequapp.com")

        // Aceptar propuesta -> pasa a RESERVADO
        repository.updateProposalStatus("prop_repo_02", TradeStatus.RESERVADO)

        val updated = repository.getProposalById("prop_repo_02")
        assertNotNull(updated)
        assertEquals("RESERVADO", updated?.trueque?.estado)
        assertTrue(updated?.estaActiva == true, "Al estar RESERVADO debe seguir activa (True)")
    }

    @Test
    @DisplayName("Emitir contraoferta (CONTRAOFERTADA) actualiza items/coins y mantiene estaActiva = true")
    fun submitCounterOffer_updatesItemsAndKeepsEstaActivaTrue() = runTest {
        val proposal = TradeProposal(
            id = "prop_repo_03",
            requesterUserId = "userA@truequapp.com",
            receiverUserId = "userB@truequapp.com",
            offeredItemIds = listOf("item_1"),
            targetItemId = "item_2",
            compensatoryTruequeCoins = 50.0,
            modality = TradeModality.HIBRIDO_CON_CREDITOS,
            status = TradeStatus.PENDIENTE
        )
        repository.saveProposal(proposal, "userA@truequapp.com", "userB@truequapp.com")

        // Contraofertar
        repository.submitCounterOffer(
            proposalId = "prop_repo_03",
            newOfferedItemIds = listOf("item_1", "item_extra"),
            newTruequeCoins = 150.0
        )

        val updated = repository.getProposalById("prop_repo_03")
        assertNotNull(updated)
        assertEquals("CONTRAOFERTADA", updated?.trueque?.estado)
        assertTrue(updated?.estaActiva == true, "Al estar CONTRAOFERTADA debe seguir activa (True)")
        assertEquals("item_1,item_extra", updated?.ofertaA?.articulosIds)
        assertEquals(150.0, updated?.ofertaA?.truequeCoins)
    }

    @Test
    @DisplayName("Cancelar propuesta actualiza el estado a CANCELADO y estaActiva = false")
    fun cancelProposal_setsEstaActivaFalse() = runTest {
        val proposal = TradeProposal(
            id = "prop_repo_04",
            requesterUserId = "userA@truequapp.com",
            receiverUserId = "userB@truequapp.com",
            offeredItemIds = listOf("item_1"),
            targetItemId = "item_2",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.PENDIENTE
        )
        repository.saveProposal(proposal, "userA@truequapp.com", "userB@truequapp.com")

        // Cancelar propuesta
        repository.updateProposalStatus("prop_repo_04", TradeStatus.CANCELADO)

        val updated = repository.getProposalById("prop_repo_04")
        assertNotNull(updated)
        assertEquals("CANCELADO", updated?.trueque?.estado)
        assertFalse(updated?.estaActiva == true, "Al estar CANCELADO la variable estaActiva debe ser FALSE")
    }

    @Test
    @DisplayName("Rechazar propuesta actualiza el estado a RECHAZADO y estaActiva = false")
    fun rejectProposal_setsEstaActivaFalse() = runTest {
        val proposal = TradeProposal(
            id = "prop_repo_05",
            requesterUserId = "userA@truequapp.com",
            receiverUserId = "userB@truequapp.com",
            offeredItemIds = listOf("item_1"),
            targetItemId = "item_2",
            modality = TradeModality.DIRECTO_1_A_1,
            status = TradeStatus.PENDIENTE
        )
        repository.saveProposal(proposal, "userA@truequapp.com", "userB@truequapp.com")

        // Rechazar propuesta
        repository.updateProposalStatus("prop_repo_05", TradeStatus.RECHAZADO)

        val updated = repository.getProposalById("prop_repo_05")
        assertNotNull(updated)
        assertEquals("RECHAZADO", updated?.trueque?.estado)
        assertFalse(updated?.estaActiva == true, "Al estar RECHAZADO la variable estaActiva debe ser FALSE")
    }

    @Test
    @DisplayName("toTradeProposal transforma correctamente TruequeConOfertas al modelo TradeProposal")
    fun toTradeProposal_transformsCorrectly() = runTest {
        val trueque = TruequeEntity(
            id = "prop_map_test",
            usuarioACorreo = "emisor@test.com",
            ofertaAId = "of_a",
            usuarioBCorreo = "receptor@test.com",
            ofertaBId = "of_b",
            estado = "RESERVADO",
            estaActiva = true
        )
        val ofA = OfertaItemEntity("of_a", "emisor@test.com", "it_1,it_2", 80.0)
        val ofB = OfertaItemEntity("of_b", "receptor@test.com", "it_dest", 0.0)

        val aggregate = TruequeConOfertas(trueque, ofA, ofB)
        val domainProp = aggregate.toTradeProposal()

        assertEquals("prop_map_test", domainProp.id)
        assertEquals("emisor@test.com", domainProp.requesterUserId)
        assertEquals("receptor@test.com", domainProp.receiverUserId)
        assertEquals(listOf("it_1", "it_2"), domainProp.offeredItemIds)
        assertEquals("it_dest", domainProp.targetItemId)
        assertEquals(80.0, domainProp.compensatoryTruequeCoins)
        assertEquals(TradeStatus.RESERVADO, domainProp.status)
        assertTrue(domainProp.estaActiva)
        assertEquals(TradeModality.HIBRIDO_CON_CREDITOS, domainProp.modality)
    }
}

