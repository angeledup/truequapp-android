package com.truequapp.feature.catalog.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.database.dao.TradeItemDao
import com.truequapp.core.database.mapper.toDomainModel
import com.truequapp.core.database.mapper.toEntity
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.rules.BusinessRuleException
import com.truequapp.core.domain.rules.ValidateProhibitedItemUseCase
import com.truequapp.feature.catalog.compressor.WebPImageCompressor
import com.truequapp.feature.catalog.storage.CatalogLocalStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel para la gestión de catálogo, filtrado geolocalizado, publicaciones persistentes en SQLite Room
 * y marcadores/favoritos para encontrar publicaciones activas rápidamente.
 */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val validateProhibitedItemUseCase: ValidateProhibitedItemUseCase = ValidateProhibitedItemUseCase(),
    private val webPImageCompressor: WebPImageCompressor = WebPImageCompressor(),
    private val catalogLocalStorage: CatalogLocalStorage? = null,
    private val tradeItemDao: TradeItemDao? = null,
    private val userSessionDataStore: UserSessionDataStore,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<CatalogUiState, CatalogUiEvent, CatalogUiEffect>(CatalogUiState()) {

    private val inMemoryItems = mutableListOf<TradeItem>()
    private var currentUserId: String = "usr_current"
    private var currentUserName: String = "Usuario Truequapp"

    init {
        loadSessionAndCatalog()
    }

    private fun loadSessionAndCatalog() {
        viewModelScope.launch(dispatchers.io) {
            userSessionDataStore.sessionFlow.collect { session ->
                currentUserId = session.userId ?: "usr_current"
                currentUserName = session.userName ?: "Usuario Truequapp"
                setState { copy(truequeCoins = session.truequeCoins) }
                syncCatalogWithDatabase()
            }
        }
    }

    private suspend fun syncCatalogWithDatabase() {
        // Cargar artículos guardados de SQLite Room
        val dbEntities = tradeItemDao?.getAllItemsSync().orEmpty()
        val loadedItems = if (dbEntities.isNotEmpty()) {
            dbEntities.map { it.toDomainModel() }
        } else {
            val seedItems = listOf(
                TradeItem(
                    id = "item_chocolatt_01",
                    ownerId = "usr_chocolatt",
                    title = "Consola Nintendo Switch OLED",
                    description = "En caja original con accesorios completos y dos mandos Joy-Con.",
                    categoryId = 2,
                    type = ItemType.BIEN_FISICO,
                    condition = ItemCondition.COMO_NUEVO,
                    estimatedValueMin = 4500.0,
                    estimatedValueMax = 5200.0,
                    imagesWebP = listOf("https://images.unsplash.com/photo-1578303512597-81e6cc155b3e"),
                    tagsWishlist = listOf("Bicicleta", "Laptop Dell", "PlayStation 4"),
                    isAvailable = true
                ),
                TradeItem(
                    id = "item_chocolatt_02",
                    ownerId = "usr_chocolatt",
                    title = "Audífonos Sony WH-1000XM4 Cancelación",
                    description = "Audífonos premium con cancelación activa de ruido y audio de alta fidelidad.",
                    categoryId = 2,
                    type = ItemType.BIEN_FISICO,
                    condition = ItemCondition.COMO_NUEVO,
                    estimatedValueMin = 3200.0,
                    estimatedValueMax = 3800.0,
                    imagesWebP = listOf("https://images.unsplash.com/photo-1546435770-a3e426bf472b"),
                    tagsWishlist = listOf("Bicicleta R29", "Gadgets"),
                    isAvailable = true
                ),
                TradeItem(
                    id = "item_vergara_01",
                    ownerId = "usr_vergara",
                    title = "Bicicleta de Montaña R29 Alum Shimano",
                    description = "Frenos de disco hidráulicos, 21 velocidades Shimano para rodar en Malecón.",
                    categoryId = 7,
                    type = ItemType.BIEN_FISICO,
                    condition = ItemCondition.USADO,
                    estimatedValueMin = 3200.0,
                    estimatedValueMax = 3800.0,
                    imagesWebP = listOf("https://images.unsplash.com/photo-1485965120184-e220f721d03e"),
                    tagsWishlist = listOf("Nintendo Switch", "Consolas", "Gaming"),
                    isAvailable = true
                ),
                TradeItem(
                    id = "item_vergara_02",
                    ownerId = "usr_vergara",
                    title = "Laptop Dell Inspiron 15 Core i5 16GB",
                    description = "Intel Core i5 11va Gen, 16GB RAM, SSD 512GB NVMe, pantalla 15.6 FHD.",
                    categoryId = 2,
                    type = ItemType.BIEN_FISICO,
                    condition = ItemCondition.COMO_NUEVO,
                    estimatedValueMin = 6500.0,
                    estimatedValueMax = 7500.0,
                    imagesWebP = listOf("https://images.unsplash.com/photo-1496181133206-80ce9b88a853"),
                    tagsWishlist = listOf("Nintendo Switch OLED", "iPad", "Gaming"),
                    isAvailable = true
                )
            )
            tradeItemDao?.insertItems(
                seedItems.map { it.toEntity(ownerName = if (it.ownerId == "usr_chocolatt") "chocolatt" else "Vergara") }
            )
            seedItems
        }

        val savedItems = catalogLocalStorage?.loadPublishedItems().orEmpty()
        val bookmarkedIds = catalogLocalStorage?.loadBookmarkedIds().orEmpty()

        inMemoryItems.clear()
        inMemoryItems.addAll(loadedItems)
        savedItems.forEach { saved ->
            if (inMemoryItems.none { it.id == saved.id }) {
                inMemoryItems.add(saved)
                tradeItemDao?.insertItem(saved.toEntity(ownerName = currentUserName))
            }
        }

        val myItems = inMemoryItems.filter { it.ownerId == currentUserId }

        setState {
            copy(
                items = inMemoryItems,
                myInventory = myItems,
                bookmarkedItemIds = bookmarkedIds
            )
        }
        applyFilters()
    }

    override fun onEvent(event: CatalogUiEvent) {
        when (event) {
            is CatalogUiEvent.SearchQueryChanged -> {
                setState { copy(searchQuery = event.query) }
                applyFilters()
            }
            is CatalogUiEvent.CategoryFilterSelected -> {
                setState { copy(selectedCategoryId = event.categoryId) }
                applyFilters()
            }
            is CatalogUiEvent.ConditionFilterSelected -> {
                setState { copy(selectedCondition = event.condition) }
                applyFilters()
            }
            is CatalogUiEvent.DistanceFilterChanged -> {
                setState { copy(maxDistanceKm = event.distanceKm) }
                applyFilters()
            }
            is CatalogUiEvent.ToggleBookmarkedFilter -> {
                setState { copy(filterOnlyBookmarked = !filterOnlyBookmarked, filterOnlyMyPublications = false) }
                applyFilters()
            }
            is CatalogUiEvent.ToggleMyPublicationsFilter -> {
                setState { copy(filterOnlyMyPublications = !filterOnlyMyPublications, filterOnlyBookmarked = false) }
                applyFilters()
            }
            is CatalogUiEvent.ToggleBookmark -> toggleBookmark(event.itemId)

            is CatalogUiEvent.TitleChanged -> setState { copy(newTitle = event.title) }
            is CatalogUiEvent.DescriptionChanged -> setState { copy(newDescription = event.description) }
            is CatalogUiEvent.CategoryChanged -> setState { copy(newCategoryId = event.categoryId) }
            is CatalogUiEvent.ConditionChanged -> setState { copy(newCondition = event.condition) }
            is CatalogUiEvent.ValueRangeChanged -> setState {
                copy(newEstimatedValueMin = event.min, newEstimatedValueMax = event.max)
            }
            is CatalogUiEvent.AddWishlistTag -> {
                if (event.tag.isNotBlank() && !currentState.newWishlistTags.contains(event.tag)) {
                    setState { copy(newWishlistTags = newWishlistTags + event.tag.trim()) }
                }
            }
            is CatalogUiEvent.RemoveWishlistTag -> {
                setState { copy(newWishlistTags = newWishlistTags - event.tag) }
            }
            is CatalogUiEvent.ImageSelected -> {
                // Estrictamente 1 sola imagen
                setState { copy(selectedImageUri = event.uri) }
            }
            is CatalogUiEvent.SubmitPublication -> submitPublication()
            is CatalogUiEvent.PauseItem -> pauseItem(event.itemId)
            is CatalogUiEvent.DeleteItem -> deleteItem(event.itemId)
            is CatalogUiEvent.DismissError -> setState { copy(errorMessage = null) }
        }
    }

    private fun toggleBookmark(itemId: String) {
        val current = currentState.bookmarkedItemIds.toMutableSet()
        val isNowBookmarked = if (current.contains(itemId)) {
            current.remove(itemId)
            false
        } else {
            current.add(itemId)
            true
        }

        // Persistir en almacenamiento local
        catalogLocalStorage?.saveBookmarkedIds(current)

        setState { copy(bookmarkedItemIds = current) }
        applyFilters()

        val msg = if (isNowBookmarked) "Artículo guardado en favoritos." else "Artículo removido de favoritos."
        sendEffect(CatalogUiEffect.ShowSnackbar(msg))
    }

    private fun applyFilters() {
        val state = currentState
        var result = state.items

        // Filtro de solo marcados/favoritos
        if (state.filterOnlyBookmarked) {
            result = result.filter { state.bookmarkedItemIds.contains(it.id) && it.status == ItemStatus.DISPONIBLE && it.ownerId != currentUserId }
        } else if (state.filterOnlyMyPublications) {
            // Filtro de solo mis publicaciones guardadas
            result = result.filter { it.ownerId == currentUserId }
        } else {
            // Feed general de exploración: Solo mostrar artículos disponibles de OTROS usuarios
            // Evita que aparezcan tus propias publicaciones como opción para intercambiar contigo mismo
            result = result.filter { it.status == ItemStatus.DISPONIBLE && it.ownerId != currentUserId }
        }

        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.tagsWishlist.any { tag -> tag.lowercase().contains(q) }
            }
        }

        if (state.selectedCategoryId != null) {
            result = result.filter { it.categoryId == state.selectedCategoryId }
        }

        if (state.selectedCondition != null) {
            result = result.filter { it.condition == state.selectedCondition }
        }

        setState { copy(filteredItems = result) }
    }

    private fun submitPublication() {
        val state = currentState

        if (state.newTitle.isBlank()) {
            setState { copy(errorMessage = "El título de la publicación es obligatorio.") }
            return
        }

        // Validación de Regla RN-11: Prohibición estricta de artículos ilícitos
        try {
            validateProhibitedItemUseCase(state.newTitle, state.newDescription)
        } catch (e: BusinessRuleException.ProhibitedItemException) {
            setState {
                copy(errorMessage = "Infracción RN-11: La publicación contiene términos no permitidos (${e.matchedKeyword}).")
            }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            setState { copy(isPublishing = true, errorMessage = null) }

            val images = if (!state.selectedImageUri.isNullOrBlank()) {
                listOf(state.selectedImageUri)
            } else {
                emptyList()
            }

            val newItem = TradeItem(
                id = "item_${UUID.randomUUID().toString().take(8)}",
                ownerId = currentUserId,
                title = state.newTitle.trim(),
                description = state.newDescription.trim(),
                categoryId = state.newCategoryId,
                type = ItemType.BIEN_FISICO,
                condition = state.newCondition,
                estimatedValueMin = state.newEstimatedValueMin,
                estimatedValueMax = state.newEstimatedValueMax,
                imagesWebP = images,
                tagsWishlist = state.newWishlistTags,
                isAvailable = true
            )

            // 1. Guardar de forma persistente en SQLite Room
            tradeItemDao?.insertItem(
                newItem.toEntity(
                    ownerName = currentUserName,
                    neighborhood = "Centro, Coatzacoalcos",
                    city = "Coatzacoalcos"
                )
            )

            inMemoryItems.add(0, newItem)

            // 2. Guardar en almacenamiento secundario sandbox
            val mySavedPublications = inMemoryItems.filter { it.ownerId == currentUserId }
            catalogLocalStorage?.savePublishedItems(mySavedPublications)

            setState {
                copy(
                    isPublishing = false,
                    items = inMemoryItems,
                    myInventory = mySavedPublications,
                    newTitle = "",
                    newDescription = "",
                    selectedImageUri = null,
                    newWishlistTags = emptyList()
                )
            }

            applyFilters()
            sendEffect(CatalogUiEffect.ShowSnackbar("¡Publicación guardada exitosamente en la base de datos!"))
            sendEffect(CatalogUiEffect.ItemPublishedSuccess)
        }
    }

    private fun pauseItem(itemId: String) {
        val item = inMemoryItems.find { it.id == itemId } ?: return
        if (item.status == ItemStatus.RESERVADO) {
            setState { copy(errorMessage = "Infracción RN-01: No puedes pausar un artículo en reserva firme.") }
            return
        }

        val updated = item.copy(status = if (item.status == ItemStatus.PAUSADO) ItemStatus.DISPONIBLE else ItemStatus.PAUSADO)
        updateItemInCatalog(updated)
    }

    private fun deleteItem(itemId: String) {
        val item = inMemoryItems.find { it.id == itemId } ?: return
        if (item.status == ItemStatus.RESERVADO) {
            setState { copy(errorMessage = "Infracción RN-01: No puedes eliminar un artículo en reserva firme.") }
            return
        }

        val updated = item.copy(status = ItemStatus.ELIMINADO)
        updateItemInCatalog(updated)
    }

    private fun updateItemInCatalog(updated: TradeItem) {
        val index = inMemoryItems.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            inMemoryItems[index] = updated
            viewModelScope.launch(dispatchers.io) {
                tradeItemDao?.updateItem(
                    updated.toEntity(
                        ownerName = currentUserName,
                        neighborhood = "Centro, Coatzacoalcos",
                        city = "Coatzacoalcos"
                    )
                )
                val mySaved = inMemoryItems.filter { it.ownerId == currentUserId }
                catalogLocalStorage?.savePublishedItems(mySaved)
            }
            val mySaved = inMemoryItems.filter { it.ownerId == currentUserId }
            setState {
                copy(
                    items = inMemoryItems,
                    myInventory = mySaved
                )
            }
            applyFilters()
        }
    }

    // Helper para testing
    fun addTestItem(item: TradeItem) {
        inMemoryItems.add(item)
        applyFilters()
    }
}
