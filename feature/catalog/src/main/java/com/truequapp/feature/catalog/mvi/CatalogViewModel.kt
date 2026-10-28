package com.truequapp.feature.catalog.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.domain.model.CategoryTaxonomy
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.TradeItem
import com.truequapp.core.domain.rules.BusinessRuleException
import com.truequapp.core.domain.rules.ValidateProhibitedItemUseCase
import com.truequapp.feature.catalog.compressor.WebPImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para la gestión de catálogo, filtrado geolocalizado de marketplace y publicación de artículos con compresión WebP.
 * Hace cumplir de manera estricta las reglas RN-11 (Prohibición de Artículos Ilícitos) y RN-01 (Reserva Firme de Inventario).
 */
@HiltViewModel
class CatalogViewModel @Inject constructor(
    private val validateProhibitedItemUseCase: ValidateProhibitedItemUseCase = ValidateProhibitedItemUseCase(),
    private val webPImageCompressor: WebPImageCompressor = WebPImageCompressor(),
    private val userSessionDataStore: UserSessionDataStore,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<CatalogUiState, CatalogUiEvent, CatalogUiEffect>(CatalogUiState()) {

    init {
        loadInitialCatalog()
    }

    private fun loadInitialCatalog() {
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
                description = "Libro universitario para ingeniería, pasta dura sin rayones.",
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
                description = "Frenos de disco hidráulicos, 21 velocidades Shimano.",
                categoryId = 7,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.USADO,
                estimatedValueMin = 3200.0,
                estimatedValueMax = 3800.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1485965120184-e220f721d03e"),
                tagsWishlist = listOf("Smartphone", "Laptop i5"),
                isAvailable = true
            ),
            TradeItem(
                id = "item_004",
                ownerId = "usr_current",
                title = "Guitarra Acústica Yamaha F310",
                description = "Excelente resonancia, funda acolchada incluida.",
                categoryId = 6,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 1800.0,
                estimatedValueMax = 2200.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1510915361894-db8b60106cb1"),
                tagsWishlist = listOf("Teclado MIDI", "Micrófono USB"),
                isAvailable = true
            )
        )

        setState {
            copy(
                items = seedItems,
                filteredItems = seedItems,
                myInventory = seedItems.filter { it.ownerId == "usr_current" }
            )
        }
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
            is CatalogUiEvent.TitleChanged -> setState { copy(newTitle = event.title, errorMessage = null) }
            is CatalogUiEvent.DescriptionChanged -> setState { copy(newDescription = event.description, errorMessage = null) }
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
                setState { copy(newWishlistTags = newWishlistTags.filter { it != event.tag }) }
            }
            is CatalogUiEvent.ImagesSelected -> {
                // Límite de 6 imágenes según especificación CU005
                val limited = event.uris.take(6)
                setState { copy(selectedImageUris = limited) }
            }
            is CatalogUiEvent.SubmitPublish -> executePublishItem()
            is CatalogUiEvent.TogglePauseItem -> togglePause(event.itemId)
            is CatalogUiEvent.DeleteItem -> deleteItem(event.itemId)
            is CatalogUiEvent.RefreshFeed -> loadInitialCatalog()
        }
    }

    private fun applyFilters() {
        val query = currentState.searchQuery.trim().lowercase()
        val catId = currentState.selectedCategoryId
        val cond = currentState.selectedCondition

        val result = currentState.items.filter { item ->
            val matchesQuery = query.isBlank() ||
                    item.title.lowercase().contains(query) ||
                    item.description.lowercase().contains(query) ||
                    item.tagsWishlist.any { it.lowercase().contains(query) }
            val matchesCategory = catId == null || item.categoryId == catId
            val matchesCondition = cond == null || item.condition == cond
            val isPublic = item.status == ItemStatus.DISPONIBLE

            matchesQuery && matchesCategory && matchesCondition && isPublic
        }
        setState { copy(filteredItems = result) }
    }

    private fun executePublishItem() {
        val title = currentState.newTitle.trim()
        val description = currentState.newDescription.trim()
        val tags = currentState.newWishlistTags

        if (title.length < 4) {
            setState { copy(errorMessage = "El título debe tener al menos 4 caracteres.") }
            return
        }
        if (description.length < 10) {
            setState { copy(errorMessage = "La descripción debe tener al menos 10 caracteres.") }
            return
        }
        if (currentState.newEstimatedValueMax < currentState.newEstimatedValueMin) {
            setState { copy(errorMessage = "El valor máximo debe ser mayor o igual al valor mínimo.") }
            return
        }

        // Validación estricta de la regla RN-11 (Prohibición de Artículos Ilícitos)
        try {
            validateProhibitedItemUseCase(
                title = title,
                description = description,
                tags = tags
            )
        } catch (ex: BusinessRuleException.ProhibitedItemException) {
            setState {
                copy(
                    errorMessage = "Infracción RN-11: Publicación rechazada. Se detectó el término prohibido '${ex.matchedKeyword}'. No se permiten armas, medicamentos controlados o artículos ilícitos."
                )
            }
            return
        }

        viewModelScope.launch(dispatchers.io) {
            setState { copy(isPublishing = true, errorMessage = null) }
            val session = userSessionDataStore.sessionFlow.first()
            val ownerId = session.userId ?: "usr_current"

            val newItem = TradeItem(
                id = "item_" + System.currentTimeMillis().toString().takeLast(6),
                ownerId = ownerId,
                title = title,
                description = description,
                categoryId = currentState.newCategoryId,
                type = ItemType.BIEN_FISICO,
                condition = currentState.newCondition,
                estimatedValueMin = currentState.newEstimatedValueMin,
                estimatedValueMax = currentState.newEstimatedValueMax,
                imagesWebP = currentState.selectedImageUris,
                tagsWishlist = tags,
                isAvailable = true
            )

            val updatedItems = listOf(newItem) + currentState.items
            val updatedInventory = listOf(newItem) + currentState.myInventory

            setState {
                copy(
                    isPublishing = false,
                    items = updatedItems,
                    filteredItems = updatedItems,
                    myInventory = updatedInventory,
                    newTitle = "",
                    newDescription = "",
                    newWishlistTags = emptyList(),
                    selectedImageUris = emptyList()
                )
            }

            sendEffect(CatalogUiEffect.ShowSnackbar("Artículo publicado con éxito en el catálogo."))
            sendEffect(CatalogUiEffect.ItemPublishedSuccess)
        }
    }

    private fun togglePause(itemId: String) {
        val target = currentState.items.find { it.id == itemId } ?: return

        // Regla RN-01: No se puede pausar un artículo en estado RESERVADO
        if (target.status == ItemStatus.RESERVADO) {
            sendEffect(CatalogUiEffect.ShowSnackbar("Infracción RN-01: El artículo está RESERVADO en una negociación activa."))
            return
        }

        val newStatus = if (target.status == ItemStatus.DISPONIBLE) ItemStatus.PAUSADO else ItemStatus.DISPONIBLE
        val updatedList = currentState.items.map {
            if (it.id == itemId) it.copy(status = newStatus) else it
        }

        setState {
            copy(
                items = updatedList,
                filteredItems = updatedList.filter { it.status == ItemStatus.DISPONIBLE },
                myInventory = updatedList.filter { it.ownerId == target.ownerId }
            )
        }
        val msg = if (newStatus == ItemStatus.PAUSADO) "Artículo pausado del marketplace." else "Artículo reactivado en marketplace."
        sendEffect(CatalogUiEffect.ShowSnackbar(msg))
    }

    private fun deleteItem(itemId: String) {
        val target = currentState.items.find { it.id == itemId } ?: return

        // Regla RN-01: Bloqueo estricto de eliminación si el artículo está RESERVADO
        if (target.status == ItemStatus.RESERVADO) {
            sendEffect(CatalogUiEffect.ShowSnackbar("Infracción RN-01: No puedes eliminar un artículo que se encuentra RESERVADO en un trueque."))
            return
        }

        val updatedList = currentState.items.filter { it.id != itemId }
        setState {
            copy(
                items = updatedList,
                filteredItems = updatedList.filter { it.status == ItemStatus.DISPONIBLE },
                myInventory = updatedList.filter { it.ownerId == target.ownerId }
            )
        }
        sendEffect(CatalogUiEffect.ShowSnackbar("Artículo eliminado de tu inventario."))
    }
}

