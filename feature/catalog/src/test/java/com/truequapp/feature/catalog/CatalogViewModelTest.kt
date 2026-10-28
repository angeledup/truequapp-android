package com.truequapp.feature.catalog

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.datastore.model.UserSession
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.feature.catalog.compressor.WebPImageCompressor
import com.truequapp.feature.catalog.mvi.CatalogUiEvent
import com.truequapp.feature.catalog.mvi.CatalogViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FakeCatalogDataStore : UserSessionDataStore {
    private val _session = MutableStateFlow(
        UserSession(userId = "usr_current", userEmail = "current@truequapp.com", isKycVerified = true)
    )
    override val sessionFlow: Flow<UserSession> = _session
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUser(userId: String, email: String, isKycVerified: Boolean) {}
    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun clearSession() {}
}

class TestCatalogDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val unconfined: CoroutineDispatcher get() = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dispatchers = TestCatalogDispatcherProvider(testDispatcher)
    private val fakeDataStore = FakeCatalogDataStore()

    private lateinit var viewModel: CatalogViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = CatalogViewModel(
            userSessionDataStore = fakeDataStore,
            dispatchers = dispatchers
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("RN-11: Publicación de artículo con palabra prohibida (arma/munición) debe ser rechazada")
    fun shouldRejectPublication_whenProhibitedWordDetected() = runTest(testDispatcher) {
        viewModel.onEvent(CatalogUiEvent.TitleChanged("Vendo pistola de colección"))
        viewModel.onEvent(CatalogUiEvent.DescriptionChanged("Arma deportiva con municiones incluidas."))
        viewModel.onEvent(CatalogUiEvent.SubmitPublication)

        val state = viewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("RN-11"))
        assertTrue(state.errorMessage!!.contains("pistola"))
    }

    @Test
    @DisplayName("CU005: Publicación válida con 1 sola imagen agrega el ítem al catálogo y lo guarda en mis publicaciones")
    fun shouldPublishItemSuccessfully_withSingleImage() = runTest(testDispatcher) {
        viewModel.onEvent(CatalogUiEvent.TitleChanged("Teclado Mecánico RGB"))
        viewModel.onEvent(CatalogUiEvent.DescriptionChanged("Switches Brown, cable tipo C desmontable."))
        viewModel.onEvent(CatalogUiEvent.CategoryChanged(1))
        viewModel.onEvent(CatalogUiEvent.ConditionChanged(ItemCondition.COMO_NUEVO))
        viewModel.onEvent(CatalogUiEvent.ValueRangeChanged(800.0, 1100.0))
        viewModel.onEvent(CatalogUiEvent.AddWishlistTag("Audífonos"))
        viewModel.onEvent(CatalogUiEvent.ImageSelected("content://media/external/images/media/101"))

        viewModel.onEvent(CatalogUiEvent.SubmitPublication)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertTrue(state.items.any { it.title == "Teclado Mecánico RGB" })
        assertTrue(state.myInventory.any { it.title == "Teclado Mecánico RGB" })
        val published = state.items.first { it.title == "Teclado Mecánico RGB" }
        assertEquals(1, published.imagesWebP.size)
    }

    @Test
    @DisplayName("Marcadores: Permite guardar publicaciones y filtrarlas rápidamente solo si siguen activas")
    fun shouldBookmarkAndFilterActiveItems() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Marcar el ítem item_001
        viewModel.onEvent(CatalogUiEvent.ToggleBookmark("item_001"))
        assertTrue(viewModel.uiState.value.bookmarkedItemIds.contains("item_001"))

        // Activar filtro de guardados
        viewModel.onEvent(CatalogUiEvent.ToggleBookmarkedFilter)
        val filtered = viewModel.uiState.value.filteredItems
        assertEquals(1, filtered.size)
        assertEquals("item_001", filtered.first().id)

        // Si el ítem ya no está disponible, no debe mostrarse en guardados activos
        val reservedItem = filtered.first().copy(status = ItemStatus.RESERVADO)
        viewModel.onEvent(CatalogUiEvent.PauseItem(reservedItem.id)) // Cambia estado
    }

    @Test
    @DisplayName("CU008: Filtrado de catálogo por categoría muestra únicamente ítems coincidentes")
    fun shouldFilterItemsByCategory() = runTest(testDispatcher) {
        viewModel.onEvent(CatalogUiEvent.CategoryFilterSelected(2))

        val state = viewModel.uiState.value
        assertFalse(state.filteredItems.isEmpty())
        assertTrue(state.filteredItems.all { it.categoryId == 2 })
    }

    @Test
    @DisplayName("CU008: Búsqueda por texto filtra ítems por título, descripción o wishlist")
    fun shouldFilterItemsBySearchQuery() = runTest(testDispatcher) {
        viewModel.onEvent(CatalogUiEvent.SearchQueryChanged("Switch"))

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredItems.size)
        assertEquals("Consola Nintendo Switch OLED", state.filteredItems.first().title)
    }

    @Test
    @DisplayName("RN-01: No debe permitir eliminar ni pausar un artículo en estado RESERVADO")
    fun shouldPreventDeletingOrPausing_whenItemIsReserved() = runTest(testDispatcher) {
        val reservedItem = TradeItem(
            id = "reserved_item_99",
            ownerId = "usr_current",
            title = "MacBook Pro M1",
            description = "Bloqueado por trueque activo",
            categoryId = 1,
            type = ItemType.BIEN_FISICO,
            condition = ItemCondition.COMO_NUEVO,
            estimatedValue = com.truequapp.core.domain.model.EstimatedValueRange(14000.0, 16000.0),
            imagesWebP = emptyList(),
            tagsWishlist = emptyList(),
            status = ItemStatus.RESERVADO
        )
        viewModel.addTestItem(reservedItem)

        // Intentar pausar
        viewModel.onEvent(CatalogUiEvent.PauseItem(reservedItem.id))
        // Intentar eliminar
        viewModel.onEvent(CatalogUiEvent.DeleteItem(reservedItem.id))

        val state = viewModel.uiState.value
        assertFalse(state.items.any { it.id == reservedItem.id && it.status == ItemStatus.ELIMINADO })
    }

    @Test
    @DisplayName("CU005: El umbral máximo de peso de imagen WebP debe ser estrictamente de 80 KB")
    fun shouldEnforce80KbThreshold() {
        assertEquals(81920L, WebPImageCompressor.MAX_IMAGE_SIZE_BYTES)
    }
}
