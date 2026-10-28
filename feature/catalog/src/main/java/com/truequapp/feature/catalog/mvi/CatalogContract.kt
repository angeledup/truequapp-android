package com.truequapp.feature.catalog.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import com.truequapp.core.domain.model.CategoryTaxonomy
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.TradeItem

/**
 * Estado inmutable de la interfaz de usuario para el módulo de Catálogo, Feed y Publicación.
 */
data class CatalogUiState(
    val isLoading: Boolean = false,
    val items: List<TradeItem> = emptyList(),
    val filteredItems: List<TradeItem> = emptyList(),
    val myInventory: List<TradeItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val selectedCondition: ItemCondition? = null,
    val maxDistanceKm: Float = 25f, // Radio geográfico de 1 a 50 km (CU008)
    // Formulario de publicación (CU005)
    val newTitle: String = "",
    val newDescription: String = "",
    val newCategoryId: Int = 1,
    val newCondition: ItemCondition = ItemCondition.COMO_NUEVO,
    val newEstimatedValueMin: Double = 200.0,
    val newEstimatedValueMax: Double = 500.0,
    val newWishlistTags: List<String> = emptyList(),
    val selectedImageUris: List<String> = emptyList(), // Hasta 6 imágenes (PhotoPicker)
    val isPublishing: Boolean = false,
    val errorMessage: String? = null
) : UiState

/**
 * Eventos disparados desde el feed de marketplace o formulario de registro de ítems.
 */
sealed interface CatalogUiEvent : UiEvent {
    data class SearchQueryChanged(val query: String) : CatalogUiEvent
    data class CategoryFilterSelected(val categoryId: Int?) : CatalogUiEvent
    data class ConditionFilterSelected(val condition: ItemCondition?) : CatalogUiEvent
    data class DistanceFilterChanged(val distanceKm: Float) : CatalogUiEvent
    data class TitleChanged(val title: String) : CatalogUiEvent
    data class DescriptionChanged(val description: String) : CatalogUiEvent
    data class CategoryChanged(val categoryId: Int) : CatalogUiEvent
    data class ConditionChanged(val condition: ItemCondition) : CatalogUiEvent
    data class ValueRangeChanged(val min: Double, val max: Double) : CatalogUiEvent
    data class AddWishlistTag(val tag: String) : CatalogUiEvent
    data class RemoveWishlistTag(val tag: String) : CatalogUiEvent
    data class ImagesSelected(val uris: List<String>) : CatalogUiEvent
    data object SubmitPublish : CatalogUiEvent
    data class TogglePauseItem(val itemId: String) : CatalogUiEvent
    data class DeleteItem(val itemId: String) : CatalogUiEvent
    data object RefreshFeed : CatalogUiEvent
}

/**
 * Efectos de navegación y notificaciones para el catálogo.
 */
sealed interface CatalogUiEffect : UiEffect {
    data class ShowSnackbar(val message: String) : CatalogUiEffect
    data class NavigateToItemDetail(val itemId: String) : CatalogUiEffect
    data object ItemPublishedSuccess : CatalogUiEffect
}

