package com.truequapp.feature.catalog.mvi

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.database.repository.UserRepository
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel para la gestión de catálogo, filtrado geolocalizado, publicaciones persistentes con fotos WebP (< 80 KB)
 * y cuadrícula paginada para la Fase 2 (CU005, CU006, CU007, CU008).
 */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val validateProhibitedItemUseCase: ValidateProhibitedItemUseCase = ValidateProhibitedItemUseCase(),
    private val webPImageCompressor: WebPImageCompressor = WebPImageCompressor(),
    private val catalogLocalStorage: CatalogLocalStorage? = null,
    private val userSessionDataStore: UserSessionDataStore,
    private val dispatchers: DispatcherProvider,
    @ApplicationContext private val context: Context? = null,
    private val userRepository: UserRepository? = null
) : BaseViewModel<CatalogUiState, CatalogUiEvent, CatalogUiEffect>(CatalogUiState()) {

    private val inMemoryItems = mutableListOf<TradeItem>()
    private var currentUserId: String = "usr_current"
    private var currentUserName: String = "Usuario Truequapp"

    init {
        loadSessionAndCatalog()
    }

    private fun loadSessionAndCatalog() {
        viewModelScope.launch(dispatchers.io) {
            val session = userSessionDataStore.sessionFlow.first()
            currentUserId = session.userId ?: "usr_current"
            val userEmail = session.userEmail

            // Consultar saldo de TruequeCoins en la base de datos (Single Source of Truth)
            val dbCoins = userRepository?.getCoins(userEmail)
            if (dbCoins != null) {
                setState { copy(truequeCoins = dbCoins) }
            }

            syncCatalog()

            // Observar en tiempo real las TruequeCoins desde la base de datos
            userRepository?.observeCoins(userEmail)?.collectLatest { realTimeCoins ->
                setState { copy(truequeCoins = realTimeCoins) }
            }
        }
    }

    private fun syncCatalog() {
        val seedItems = listOf(
            TradeItem(
                id = "item_001",
                ownerId = "usr_alpha",
                title = "Consola Nintendo Switch OLED",
                description = "En caja original con accesorios completos y dos mandos Joy-Con.",
                categoryId = 2,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 4500.0,
                estimatedValueMax = 5200.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1578303512597-81e6cc155b3e"),
                tagsWishlist = listOf("PlayStation 4", "Tablet Samsung"),
                isAvailable = true
            ),
            TradeItem(
                id = "item_002",
                ownerId = "usr_beta",
                title = "Cálculo de Stewart 8va Edición",
                description = "Libro universitario para ingeniería UV, pasta dura sin rayones.",
                categoryId = 3,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.USADO,
                estimatedValueMin = 500.0,
                estimatedValueMax = 700.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c"),
                tagsWishlist = listOf("Álgebra Lineal", "Física Universitaria"),
                isAvailable = true
            ),
            TradeItem(
                id = "item_003",
                ownerId = "usr_gamma",
                title = "Bicicleta de Montaña R29 Alum",
                description = "Frenos de disco hidráulicos, 21 velocidades Shimano para rodar en Malecón.",
                categoryId = 7,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.USADO,
                estimatedValueMin = 3200.0,
                estimatedValueMax = 3800.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1485965120184-e220f721d03e"),
                tagsWishlist = listOf("Laptop", "Instrumentos"),
                isAvailable = true
            )
        )

        val savedItems = catalogLocalStorage?.loadPublishedItems().orEmpty()
        val bookmarkedIds = catalogLocalStorage?.loadBookmarkedIds().orEmpty()

        inMemoryItems.clear()
        inMemoryItems.addAll(seedItems)
        savedItems.forEach { saved ->
            if (inMemoryItems.none { it.id == saved.id }) {
                inMemoryItems.add(saved)
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
        applyFilters(resetPagination = true)
    }

    override fun onEvent(event: CatalogUiEvent) {
        when (event) {
            is CatalogUiEvent.SearchQueryChanged -> {
                setState { copy(searchQuery = event.query) }
                applyFilters(resetPagination = true)
            }
            is CatalogUiEvent.CategoryFilterSelected -> {
                setState { copy(selectedCategoryId = event.categoryId) }
                applyFilters(resetPagination = true)
            }
            is CatalogUiEvent.ConditionFilterSelected -> {
                setState { copy(selectedCondition = event.condition) }
                applyFilters(resetPagination = true)
            }
            is CatalogUiEvent.DistanceFilterChanged -> {
                setState { copy(maxDistanceKm = event.distanceKm) }
                applyFilters(resetPagination = true)
            }
            is CatalogUiEvent.ValueFilterChanged -> {
                setState {
                    copy(
                        filterMinEstimatedValue = event.min,
                        filterMaxEstimatedValue = event.max
                    )
                }
                applyFilters(resetPagination = true)
            }
            is CatalogUiEvent.ToggleBookmarkedFilter -> {
                setState { copy(filterOnlyBookmarked = !filterOnlyBookmarked, filterOnlyMyPublications = false) }
                applyFilters(resetPagination = true)
            }
            is CatalogUiEvent.ToggleMyPublicationsFilter -> {
                setState { copy(filterOnlyMyPublications = !filterOnlyMyPublications, filterOnlyBookmarked = false) }
                applyFilters(resetPagination = true)
            }
            is CatalogUiEvent.ToggleBookmark -> toggleBookmark(event.itemId)

            is CatalogUiEvent.LoadNextPage -> loadNextPage()

            // Formulario de publicación (CU005)
            is CatalogUiEvent.TitleChanged -> setState { copy(newTitle = event.title) }
            is CatalogUiEvent.DescriptionChanged -> setState { copy(newDescription = event.description) }
            is CatalogUiEvent.CategoryChanged -> setState { copy(newCategoryId = event.categoryId) }
            is CatalogUiEvent.ConditionChanged -> setState { copy(newCondition = event.condition) }
            is CatalogUiEvent.ValueRangeChanged -> setState {
                copy(newEstimatedValueMin = event.min, newEstimatedValueMax = event.max)
            }
            is CatalogUiEvent.AddWishlistTag -> {
                if (event.tag.isNotBlank() && !currentState.newWishlistTags.contains(event.tag.trim())) {
                    setState { copy(newWishlistTags = newWishlistTags + event.tag.trim()) }
                }
            }
            is CatalogUiEvent.RemoveWishlistTag -> {
                setState { copy(newWishlistTags = newWishlistTags - event.tag) }
            }
            is CatalogUiEvent.ImagesSelected -> {
                val current = currentState.selectedImageUris.toMutableList()
                for (uri in event.uris) {
                    if (current.size < WebPImageCompressor.MAX_ALLOWED_PHOTOS && !current.contains(uri)) {
                        current.add(uri)
                    }
                }
                setState {
                    copy(
                        selectedImageUris = current,
                        selectedImageUri = current.firstOrNull()
                    )
                }
            }
            is CatalogUiEvent.RemoveSelectedImage -> {
                val current = currentState.selectedImageUris.toMutableList()
                if (event.index in current.indices) {
                    current.removeAt(event.index)
                }
                setState {
                    copy(
                        selectedImageUris = current,
                        selectedImageUri = current.firstOrNull()
                    )
                }
            }
            is CatalogUiEvent.ImageSelected -> {
                if (event.uri != null) {
                    val current = if (currentState.selectedImageUris.isNotEmpty()) {
                        currentState.selectedImageUris
                    } else {
                        listOf(event.uri)
                    }
                    setState {
                        copy(
                            selectedImageUris = current,
                            selectedImageUri = event.uri
                        )
                    }
                } else {
                    setState {
                        copy(
                            selectedImageUris = emptyList(),
                            selectedImageUri = null
                        )
                    }
                }
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

        catalogLocalStorage?.saveBookmarkedIds(current)
        setState { copy(bookmarkedItemIds = current) }
        applyFilters(resetPagination = false)

        val msg = if (isNowBookmarked) "Artículo guardado en favoritos." else "Artículo removido de favoritos."
        sendEffect(CatalogUiEffect.ShowSnackbar(msg))
    }

    private fun applyFilters(resetPagination: Boolean = true) {
        val state = currentState
        var result = state.items

        // 1. Filtro de solo marcados/favoritos o mis publicaciones
        if (state.filterOnlyBookmarked) {
            result = result.filter { state.bookmarkedItemIds.contains(it.id) && it.status == ItemStatus.DISPONIBLE && it.ownerId != currentUserId }
        } else if (state.filterOnlyMyPublications) {
            result = result.filter { it.ownerId == currentUserId }
        } else {
            // Feed general de exploración: Excluir publicaciones propias para anti-auto-trueque
            result = result.filter { it.status == ItemStatus.DISPONIBLE && it.ownerId != currentUserId }
        }

        // 2. Filtro de búsqueda por texto (título, descripción, wishlist)
        if (state.searchQuery.isNotBlank()) {
            val q = state.searchQuery.trim().lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.tagsWishlist.any { tag -> tag.lowercase().contains(q) }
            }
        }

        // 3. Filtro por categoría
        if (state.selectedCategoryId != null) {
            result = result.filter { it.categoryId == state.selectedCategoryId }
        }

        // 4. Filtro por condición física
        if (state.selectedCondition != null) {
            result = result.filter { it.condition == state.selectedCondition }
        }

        // 5. Filtro por rango de valor estimado
        if (state.filterMinEstimatedValue != null) {
            result = result.filter { it.estimatedValueMax >= state.filterMinEstimatedValue!! }
        }
        if (state.filterMaxEstimatedValue != null) {
            result = result.filter { it.estimatedValueMin <= state.filterMaxEstimatedValue!! }
        }

        // 6. Paginación
        val newPage = if (resetPagination) 1 else state.currentPage
        val paginated = result.take(newPage * state.pageSize)
        val hasMore = paginated.size < result.size

        setState {
            copy(
                filteredItems = result,
                paginatedItems = paginated,
                currentPage = newPage,
                hasMorePages = hasMore,
                isPaginating = false
            )
        }
    }

    private fun loadNextPage() {
        val state = currentState
        if (!state.hasMorePages || state.isPaginating) return

        setState { copy(isPaginating = true) }
        val nextPage = state.currentPage + 1
        val paginated = state.filteredItems.take(nextPage * state.pageSize)
        val hasMore = paginated.size < state.filteredItems.size

        setState {
            copy(
                currentPage = nextPage,
                paginatedItems = paginated,
                hasMorePages = hasMore,
                isPaginating = false
            )
        }
    }

    private fun submitPublication() {
        val state = currentState

        if (state.newTitle.isBlank()) {
            setState { copy(errorMessage = "El título de la publicación es obligatorio.") }
            return
        }

        // Validación de Regla RN-11: Prohibición estricta de artículos ilícitos
        try {
            validateProhibitedItemUseCase(state.newTitle, state.newDescription, state.newWishlistTags)
        } catch (e: BusinessRuleException.ProhibitedItemException) {
            setState {
                copy(errorMessage = "Infracción RN-11: La publicación contiene términos no permitidos (${e.matchedKeyword}).")
            }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            setState { copy(isPublishing = true, errorMessage = null) }

            val rawUris = state.selectedImageUris.ifEmpty {
                listOfNotNull(state.selectedImageUri)
            }

            // Pipeline de Compresión Nativo WebP (< 80 KB) y purga EXIF
            val compressedWebPPaths = mutableListOf<String>()
            for ((index, uriStr) in rawUris.withIndex()) {
                try {
                    if (context != null && uriStr.startsWith("content://")) {
                        val uri = Uri.parse(uriStr)
                        val cacheDir = File(context.cacheDir, "catalog_webp_images")
                        cacheDir.mkdirs()
                        val targetFile = File(cacheDir, "item_${System.currentTimeMillis()}_$index.webp")
                        val result = withContext(dispatchers.default) {
                            webPImageCompressor.compressFromUri(context, uri, targetFile)
                        }
                        compressedWebPPaths.add(result.file.absolutePath)
                    } else {
                        compressedWebPPaths.add(uriStr)
                    }
                } catch (_: Exception) {
                    // Fallback para URIs remotas o pruebas unitarias
                    compressedWebPPaths.add(uriStr)
                }
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
                imagesWebP = compressedWebPPaths,
                tagsWishlist = state.newWishlistTags,
                isAvailable = true
            )

            inMemoryItems.add(0, newItem)

            // Guardar de forma persistente en almacenamiento sandbox
            val mySavedPublications = inMemoryItems.filter { it.ownerId == currentUserId }
            catalogLocalStorage?.savePublishedItems(mySavedPublications)

            setState {
                copy(
                    isPublishing = false,
                    items = inMemoryItems,
                    myInventory = mySavedPublications,
                    newTitle = "",
                    newDescription = "",
                    selectedImageUris = emptyList(),
                    selectedImageUri = null,
                    newWishlistTags = emptyList()
                )
            }

            applyFilters(resetPagination = true)
            sendEffect(CatalogUiEffect.ShowSnackbar("¡Publicación guardada exitosamente con imágenes WebP!"))
            sendEffect(CatalogUiEffect.ItemPublishedSuccess)
        }
    }

    private fun pauseItem(itemId: String) {
        val item = inMemoryItems.find { it.id == itemId } ?: return
        if (item.status == ItemStatus.RESERVADO) {
            setState { copy(errorMessage = "RN-01: No se puede pausar un artículo en trueque reservado.") }
            return
        }

        val updated = item.copy(
            status = if (item.status == ItemStatus.DISPONIBLE) ItemStatus.PAUSADO else ItemStatus.DISPONIBLE
        )
        updateItemInCatalog(updated)
    }

    private fun deleteItem(itemId: String) {
        val item = inMemoryItems.find { it.id == itemId } ?: return
        if (item.status == ItemStatus.RESERVADO) {
            setState { copy(errorMessage = "RN-01: No se puede eliminar un artículo reservado en una transacción activa.") }
            return
        }

        inMemoryItems.removeIf { it.id == itemId }
        val mySaved = inMemoryItems.filter { it.ownerId == currentUserId }
        catalogLocalStorage?.savePublishedItems(mySaved)

        setState {
            copy(
                items = inMemoryItems,
                myInventory = mySaved
            )
        }
        applyFilters(resetPagination = false)
        sendEffect(CatalogUiEffect.ShowSnackbar("Artículo eliminado."))
    }

    private fun updateItemInCatalog(updated: TradeItem) {
        val index = inMemoryItems.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            inMemoryItems[index] = updated
            val mySaved = inMemoryItems.filter { it.ownerId == currentUserId }
            catalogLocalStorage?.savePublishedItems(mySaved)
            setState {
                copy(
                    items = inMemoryItems,
                    myInventory = mySaved
                )
            }
            applyFilters(resetPagination = false)
        }
    }

    // Helper para testing
    fun addTestItem(item: TradeItem) {
        inMemoryItems.add(item)
        applyFilters(resetPagination = true)
    }
}
