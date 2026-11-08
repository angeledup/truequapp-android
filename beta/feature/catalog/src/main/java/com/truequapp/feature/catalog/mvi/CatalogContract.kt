package com.truequapp.feature.catalog.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.TradeItem

/**
 * Estado inmutable de la interfaz de usuario para el módulo de Catálogo, Feed, Publicación y Guardados (Fase 2).
 */
data class CatalogUiState(
    val isLoading: Boolean = false,
    val items: List<TradeItem> = emptyList(),
    val filteredItems: List<TradeItem> = emptyList(),
    val paginatedItems: List<TradeItem> = emptyList(),
    val myInventory: List<TradeItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val selectedCondition: ItemCondition? = null,
    val maxDistanceKm: Float = 25f, // Radio geográfico de 1 a 50 km (CU008)
    val filterMinEstimatedValue: Double? = null,
    val filterMaxEstimatedValue: Double? = null,

    // Paginación de Marketplace Feed (Paging)
    val currentPage: Int = 1,
    val pageSize: Int = 10,
    val hasMorePages: Boolean = true,
    val isPaginating: Boolean = false,

    // Filtros especiales: Marcados/Favoritos y Mis Publicaciones
    val bookmarkedItemIds: Set<String> = emptySet(),
    val filterOnlyBookmarked: Boolean = false,
    val filterOnlyMyPublications: Boolean = false,

    // Formulario de publicación (CU005): Hasta 6 fotografías con compresión WebP
    val newTitle: String = "",
    val newDescription: String = "",
    val newCategoryId: Int = 1,
    val newCondition: ItemCondition = ItemCondition.COMO_NUEVO,
    val newEstimatedValueMin: Double = 200.0,
    val newEstimatedValueMax: Double = 500.0,
    val newWishlistTags: List<String> = emptyList(),
    val selectedImageUris: List<String> = emptyList(), // Hasta 6 fotos (PickMultipleVisualMedia)
    val selectedImageUri: String? = null, // Retrocompatibilidad para foto principal
    val isPublishing: Boolean = false,
    val errorMessage: String? = null,
    val truequeCoins: Double = 10000.0 // Variable canónica unificada
) : UiState

/**
 * Eventos disparados desde el feed de marketplace o formulario de registro de ítems.
 */
sealed interface CatalogUiEvent : UiEvent {
    data class SearchQueryChanged(val query: String) : CatalogUiEvent
    data class CategoryFilterSelected(val categoryId: Int?) : CatalogUiEvent
    data class ConditionFilterSelected(val condition: ItemCondition?) : CatalogUiEvent
    data class DistanceFilterChanged(val distanceKm: Float) : CatalogUiEvent
    data class ValueFilterChanged(val min: Double?, val max: Double?) : CatalogUiEvent
    data object ToggleBookmarkedFilter : CatalogUiEvent
    data object ToggleMyPublicationsFilter : CatalogUiEvent
    data class ToggleBookmark(val itemId: String) : CatalogUiEvent
    data object LoadNextPage : CatalogUiEvent

    // Formulario de publicación (Selector nativo de hasta 6 fotos)
    data class TitleChanged(val title: String) : CatalogUiEvent
    data class DescriptionChanged(val description: String) : CatalogUiEvent
    data class CategoryChanged(val categoryId: Int) : CatalogUiEvent
    data class ConditionChanged(val condition: ItemCondition) : CatalogUiEvent
    data class ValueRangeChanged(val min: Double, val max: Double) : CatalogUiEvent
    data class AddWishlistTag(val tag: String) : CatalogUiEvent
    data class RemoveWishlistTag(val tag: String) : CatalogUiEvent
    data class ImagesSelected(val uris: List<String>) : CatalogUiEvent
    data class RemoveSelectedImage(val index: Int) : CatalogUiEvent
    data class ImageSelected(val uri: String?) : CatalogUiEvent // Retrocompatibilidad
    data object SubmitPublication : CatalogUiEvent

    data class PauseItem(val itemId: String) : CatalogUiEvent
    data class DeleteItem(val itemId: String) : CatalogUiEvent
    data object DismissError : CatalogUiEvent
}

/**
 * Efectos de un solo disparo hacia la UI.
 */
sealed interface CatalogUiEffect : UiEffect {
    data class ShowSnackbar(val message: String) : CatalogUiEffect
    data class NavigateToItemDetail(val itemId: String) : CatalogUiEffect
    data object ItemPublishedSuccess : CatalogUiEffect
}
