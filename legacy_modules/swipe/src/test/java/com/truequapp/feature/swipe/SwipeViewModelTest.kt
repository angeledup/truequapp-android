package com.truequapp.feature.swipe

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.feature.swipe.mvi.SwipeUiEffect
import com.truequapp.feature.swipe.mvi.SwipeUiEvent
import com.truequapp.feature.swipe.mvi.SwipeViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TestSwipeDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val unconfined: CoroutineDispatcher get() = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class SwipeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SwipeViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SwipeViewModel(TestSwipeDispatcherProvider(testDispatcher))
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialState loads deck with default items`() {
        val state = viewModel.uiState.value
        assertFalse(state.itemsDeck.isEmpty())
        assertEquals(0, state.currentIndex)
        assertNotNull(state.currentTopItem)
        assertTrue(state.hasRemainingItems)
    }

    @Test
    fun `swipeLeft increments index and adds item to passedItems`() {
        val initialItem = viewModel.uiState.value.currentTopItem!!
        viewModel.onEvent(SwipeUiEvent.SwipeLeft(initialItem))

        val updatedState = viewModel.uiState.value
        assertEquals(1, updatedState.currentIndex)
        assertTrue(updatedState.passedItemIds.contains(initialItem.id))
        assertFalse(updatedState.likedItemIds.contains(initialItem.id))
    }

    @Test
    fun `swipeRight adds item to likedItems`() {
        // Item 1 is not a match trigger
        val item1 = viewModel.uiState.value.itemsDeck[0]
        viewModel.onEvent(SwipeUiEvent.SwipeRight(item1))

        val updatedState = viewModel.uiState.value
        assertEquals(1, updatedState.currentIndex)
        assertTrue(updatedState.likedItemIds.contains(item1.id))
        assertFalse(updatedState.showMatchDialog)
    }

    @Test
    fun `swipeRight on match trigger item opens dialog and triggers celebration effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<SwipeUiEffect>()
        val job = launch {
            viewModel.uiEffect.toList(effects)
        }

        // swipe-item-2 is in MATCH_TRIGGER_ITEM_IDS
        val matchItem = viewModel.uiState.value.itemsDeck.first { it.id == "swipe-item-2" }
        viewModel.onEvent(SwipeUiEvent.SwipeRight(matchItem))
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertTrue(updatedState.showMatchDialog)
        assertEquals(matchItem.id, updatedState.matchedItem?.id)
        assertTrue(effects.any { it is SwipeUiEffect.TriggerMatchCelebration })

        job.cancel()
    }

    @Test
    fun `dismissMatchDialog closes modal dialog`() {
        val matchItem = viewModel.uiState.value.itemsDeck.first { it.id == "swipe-item-2" }
        viewModel.onEvent(SwipeUiEvent.SwipeRight(matchItem))
        assertTrue(viewModel.uiState.value.showMatchDialog)

        viewModel.onEvent(SwipeUiEvent.DismissMatchDialog)
        assertFalse(viewModel.uiState.value.showMatchDialog)
        assertNull(viewModel.uiState.value.matchedItem)
    }

    @Test
    fun `proposeTradeFromMatch dismisses dialog and emits NavigateToProposal effect`() = runTest(testDispatcher) {
        val effects = mutableListOf<SwipeUiEffect>()
        val job = launch {
            viewModel.uiEffect.toList(effects)
        }

        val targetItemId = "swipe-item-2"
        viewModel.onEvent(SwipeUiEvent.ProposeTradeFromMatch(targetItemId))
        advanceUntilIdle()

        val navEffect = effects.filterIsInstance<SwipeUiEffect.NavigateToProposal>().firstOrNull()
        assertNotNull(navEffect)
        assertEquals(targetItemId, navEffect?.targetItemId)

        job.cancel()
    }

    @Test
    fun `loadFeed filters out items owned by the active user`() = runTest(testDispatcher) {
        val activeUserId = "user-vendor-101" // Owner of swipe-item-1
        viewModel.onEvent(SwipeUiEvent.LoadFeed(activeUserId))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.itemsDeck.any { it.ownerId == activeUserId })
    }
}
