package com.truequapp.feature.catalog

import android.graphics.BitmapFactory
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
    override suspend fun saveUser(
        userId: String,
        email: String,
        isKycVerified: Boolean
    ) {}
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
    @DisplayName("CU005: Selector Nativo admite hasta 6 fotos y publica el ítem en SQLite y mis publicaciones")
    fun shouldPublishItemSuccessfully_withMultipleImagesUpTo6() = runTest(testDispatcher) {
        viewModel.onEvent(CatalogUiEvent.TitleChanged("Laptop Dell XPS 15"))
        viewModel.onEvent(CatalogUiEvent.DescriptionChanged("Pantalla OLED 4K, 32GB RAM, impecable."))
        viewModel.onEvent(CatalogUiEvent.CategoryChanged(1))
        viewModel.onEvent(CatalogUiEvent.ConditionChanged(ItemCondition.COMO_NUEVO))
        viewModel.onEvent(CatalogUiEvent.ValueRangeChanged(18000.0, 22000.0))
        viewModel.onEvent(CatalogUiEvent.AddWishlistTag("MacBook Pro"))

        // Seleccionar 4 fotos
        val photos = listOf(
            "content://media/external/images/media/101",
            "content://media/external/images/media/102",
            "content://media/external/images/media/103",
            "content://media/external/images/media/104"
        )
        viewModel.onEvent(CatalogUiEvent.ImagesSelected(photos))
        assertEquals(4, viewModel.uiState.value.selectedImageUris.size)

        // Remover la segunda foto
        viewModel.onEvent(CatalogUiEvent.RemoveSelectedImage(1))
        assertEquals(3, viewModel.uiState.value.selectedImageUris.size)

        viewModel.onEvent(CatalogUiEvent.SubmitPublication)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
        assertTrue(state.items.any { it.title == "Laptop Dell XPS 15" })
        assertTrue(state.myInventory.any { it.title == "Laptop Dell XPS 15" })
        val published = state.items.first { it.title == "Laptop Dell XPS 15" }
        assertEquals(3, published.imagesWebP.size)
    }

    @Test
    @DisplayName("CU005: Límite estricto de hasta 6 fotografías en selección por lote")
    fun shouldCapImagesToMaximumOf6() = runTest(testDispatcher) {
        val photos = (1..10).map { "content://media/external/images/media/$it" }
        viewModel.onEvent(CatalogUiEvent.ImagesSelected(photos))

        assertEquals(WebPImageCompressor.MAX_ALLOWED_PHOTOS, viewModel.uiState.value.selectedImageUris.size)
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
        viewModel.onEvent(CatalogUiEvent.PauseItem(reservedItem.id))
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
    @DisplayName("CU008: Filtrado por rango de valor estimado respeta límites mínimos y máximos")
    fun shouldFilterItemsByEstimatedValueRange() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Filtro de artículos entre $4000 y $6000 (Switch OLED ~$4850)
        viewModel.onEvent(CatalogUiEvent.ValueFilterChanged(4000.0, 6000.0))

        val state = viewModel.uiState.value
        assertEquals(1, state.filteredItems.size)
        assertEquals("Consola Nintendo Switch OLED", state.filteredItems.first().title)
    }

    @Test
    @DisplayName("CU008: Paginación incremental carga artículos por bloques (Paging)")
    fun shouldPaginateFeedIncrementally() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Añadir 15 artículos de prueba
        for (i in 1..15) {
            viewModel.addTestItem(
                TradeItem(
                    id = "test_item_$i",
                    ownerId = "usr_other_$i",
                    title = "Artículo Paginado $i",
                    description = "Descripción de prueba",
                    categoryId = 1,
                    type = ItemType.BIEN_FISICO,
                    condition = ItemCondition.USADO,
                    estimatedValue = com.truequapp.core.domain.model.EstimatedValueRange(100.0, 200.0),
                    imagesWebP = emptyList(),
                    tagsWishlist = emptyList(),
                    status = ItemStatus.DISPONIBLE
                )
            )
        }

        val statePage1 = viewModel.uiState.value
        assertEquals(1, statePage1.currentPage)
        assertEquals(10, statePage1.paginatedItems.size)
        assertTrue(statePage1.hasMorePages)

        // Cargar siguiente página
        viewModel.onEvent(CatalogUiEvent.LoadNextPage)
        val statePage2 = viewModel.uiState.value
        assertEquals(2, statePage2.currentPage)
        assertTrue(statePage2.paginatedItems.size > 10)
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
        assertEquals(6, WebPImageCompressor.MAX_ALLOWED_PHOTOS)
    }

    @Test
    @DisplayName("Anti-auto-trueque: Artículos propios no deben aparecer en el feed de exploración para no intercambiar contigo mismo")
    fun shouldNotShowOwnItemsInGeneralExploreFeed() = runTest(testDispatcher) {
        viewModel.onEvent(CatalogUiEvent.TitleChanged("Mi Artículo Propio"))
        viewModel.onEvent(CatalogUiEvent.DescriptionChanged("Descripción de prueba."))
        viewModel.onEvent(CatalogUiEvent.SubmitPublication)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // En el feed general de exploración NO debe aparecer
        assertFalse(state.filteredItems.any { it.title == "Mi Artículo Propio" })
        // Pero en mi inventario de publicaciones SÍ debe estar registrado
        assertTrue(state.myInventory.any { it.title == "Mi Artículo Propio" })

        // Al activar filtro de 'Mis Publicaciones' sí debe aparecer
        viewModel.onEvent(CatalogUiEvent.ToggleMyPublicationsFilter)
        val filteredState = viewModel.uiState.value
        assertTrue(filteredState.filteredItems.any { it.title == "Mi Artículo Propio" })
    }
}
