package com.truequapp.feature.swipe.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.core.database.dao.TradeItemDao
import com.truequapp.core.database.mapper.toDomainModel
import com.truequapp.core.datastore.UserSessionDataStore
import com.truequapp.core.domain.model.ItemCondition
import com.truequapp.core.domain.model.ItemStatus
import com.truequapp.core.domain.model.ItemType
import com.truequapp.core.domain.model.TradeItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel para el Modo Exploración Swipe (CU017 / Fase 4).
 * Gestiona el carrusel de tarjetas, física de gestos, descarte/interés y detección de Matches bilaterales.
 */
@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val userSessionDataStore: UserSessionDataStore? = null,
    private val tradeItemDao: TradeItemDao? = null
) : BaseViewModel<SwipeUiState, SwipeUiEvent, SwipeUiEffect>(SwipeUiState()) {

    init {
        loadInitialDeck()
        observeUserSession()
    }

    private fun observeUserSession() {
        if (userSessionDataStore != null) {
            viewModelScope.launch(dispatchers.io) {
                userSessionDataStore.sessionFlow.collect { session ->
                    val userId = session.userId ?: "usr_current"
                    loadFeedForUser(userId)
                }
            }
        }
    }

    override fun onEvent(event: SwipeUiEvent) {
        when (event) {
            is SwipeUiEvent.LoadFeed -> loadFeedForUser(event.currentUserId)
            is SwipeUiEvent.SwipeRight -> handleSwipeRight(event.item)
            is SwipeUiEvent.SwipeLeft -> handleSwipeLeft(event.item)
            is SwipeUiEvent.DismissMatchDialog -> setState {
                copy(showMatchDialog = false, matchedItem = null)
            }
            is SwipeUiEvent.ProposeTradeFromMatch -> {
                setState { copy(showMatchDialog = false, matchedItem = null) }
                sendEffect(SwipeUiEffect.NavigateToProposal(event.targetItemId))
            }
            is SwipeUiEvent.DismissError -> setState { copy(errorMessage = null) }
        }
    }

    private fun loadFeedForUser(userId: String) {
        setState { copy(currentUserId = userId, isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            val dbItems = tradeItemDao?.getAllItemsSync().orEmpty().map { it.toDomainModel() }
            val combinedDeck = mutableListOf<TradeItem>()
            combinedDeck.addAll(DEFAULT_SWIPE_ITEMS)
            dbItems.forEach { dbItem ->
                if (combinedDeck.none { it.id == dbItem.id }) {
                    combinedDeck.add(dbItem)
                }
            }

            // Filtrar estrictamente artículos que pertenezcan a OTROS usuarios (no mostrar publicaciones propias)
            val filtered = combinedDeck.filter {
                it.ownerId != userId &&
                it.status == ItemStatus.DISPONIBLE &&
                it.id !in currentState.likedItemIds &&
                it.id !in currentState.passedItemIds
            }
            setState {
                copy(
                    itemsDeck = filtered,
                    currentIndex = 0,
                    isLoading = false
                )
            }
        }
    }

    private fun handleSwipeRight(item: TradeItem) {
        val newLiked = currentState.likedItemIds + item.id
        val nextIndex = currentState.currentIndex + 1

        // Verificamos si este artículo genera Match mutuo (CU017)
        val isMutualMatch = MATCH_TRIGGER_ITEM_IDS.contains(item.id) ||
                item.tagsWishlist.any { it.contains("Match", ignoreCase = true) }

        if (isMutualMatch) {
            setState {
                copy(
                    likedItemIds = newLiked,
                    currentIndex = nextIndex,
                    matchedItem = item,
                    showMatchDialog = true
                )
            }
            sendEffect(SwipeUiEffect.TriggerMatchCelebration)
            sendEffect(SwipeUiEffect.ShowToast("¡Trueque Mutuo Encontrado!"))
        } else {
            setState {
                copy(
                    likedItemIds = newLiked,
                    currentIndex = nextIndex
                )
            }
            sendEffect(SwipeUiEffect.ShowToast("Marcado como Me Interesa"))
        }
    }

    private fun handleSwipeLeft(item: TradeItem) {
        val newPassed = currentState.passedItemIds + item.id
        val nextIndex = currentState.currentIndex + 1
        setState {
            copy(
                passedItemIds = newPassed,
                currentIndex = nextIndex
            )
        }
    }

    private fun loadInitialDeck() {
        setState {
            copy(
                itemsDeck = DEFAULT_SWIPE_ITEMS,
                currentIndex = 0,
                isLoading = false
            )
        }
    }

    companion object {
        val MATCH_TRIGGER_ITEM_IDS = setOf("swipe-item-2", "swipe-item-4")

        val DEFAULT_SWIPE_ITEMS = listOf(
            TradeItem(
                id = "swipe-item-1",
                ownerId = "user-vendor-101",
                title = "Nintendo Switch OLED 64GB",
                description = "Consola en perfecto estado con dock, cables originales y funda rígida. Busco laptop o smartphone gama media.",
                categoryId = 2,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 4800.0,
                estimatedValueMax = 5500.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1578301978693-85fa9c0320b9"),
                tagsWishlist = listOf("Consolas", "Laptop", "Gaming")
            ),
            TradeItem(
                id = "swipe-item-2",
                ownerId = "user-vendor-102",
                title = "Bicicleta de Ruta Alubike R28",
                description = "Bicicleta de cuadro de aluminio ultraligero, grupo Shimano Claris 2x8. Lista para rodar en carretera.",
                categoryId = 7,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.USADO,
                estimatedValueMin = 4500.0,
                estimatedValueMax = 5200.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1485965120184-e220f721d03e"),
                tagsWishlist = listOf("Bicicletas", "Match Directo", "Deportes")
            ),
            TradeItem(
                id = "swipe-item-3",
                ownerId = "user-vendor-103",
                title = "Cámara Mirrorless Sony Alpha a6000",
                description = "Incluye lente 16-50mm, 2 baterías y memoria de 64GB. Excelente para fotografía de producto y video.",
                categoryId = 1,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.COMO_NUEVO,
                estimatedValueMin = 5000.0,
                estimatedValueMax = 6000.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1516035069371-29a1b244cc32"),
                tagsWishlist = listOf("Fotografía", "Audio", "Laptops")
            ),
            TradeItem(
                id = "swipe-item-4",
                ownerId = "user-vendor-104",
                title = "Guitarra Electroacústica Fender FA-125CE",
                description = "Sonido brillante, pastilla Fishman integrada con afinador. Incluye funda acolchada y tahalí.",
                categoryId = 6,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.USADO,
                estimatedValueMin = 3200.0,
                estimatedValueMax = 3900.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1510915361894-db8b60106cb1"),
                tagsWishlist = listOf("Instrumentos", "Match Directo", "Amplificadores")
            ),
            TradeItem(
                id = "swipe-item-5",
                ownerId = "user-vendor-105",
                title = "Kit Taladro Percutor Brushless Dewalt 20V",
                description = "Incluye 2 baterías de 2Ah, cargador rápido y maletín de uso rudo. Ideal para herrería y carpintería.",
                categoryId = 5,
                type = ItemType.BIEN_FISICO,
                condition = ItemCondition.NUEVO,
                estimatedValueMin = 2800.0,
                estimatedValueMax = 3400.0,
                imagesWebP = listOf("https://images.unsplash.com/photo-1504148455328-c376907d081c"),
                tagsWishlist = listOf("Herramientas", "Taller", "Hogar")
            )
        )
    }
}
