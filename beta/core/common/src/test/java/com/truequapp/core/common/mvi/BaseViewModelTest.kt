package com.truequapp.core.common.mvi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// Test MVI contracts
data class SampleState(val counter: Int = 0) : UiState
sealed interface SampleEvent : UiEvent {
    data object Increment : SampleEvent
    data class SendNotice(val text: String) : SampleEvent
}
sealed interface SampleEffect : UiEffect {
    data class ShowToast(val message: String) : SampleEffect
}

class SampleViewModel : BaseViewModel<SampleState, SampleEvent, SampleEffect>(SampleState()) {
    override fun onEvent(event: SampleEvent) {
        when (event) {
            SampleEvent.Increment -> setState { copy(counter = counter + 1) }
            is SampleEvent.SendNotice -> sendEffect(SampleEffect.ShowToast(event.text))
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val viewModel = SampleViewModel()
        assertEquals(0, viewModel.uiState.value.counter)
    }

    @Test
    fun `onEvent Increment updates state atomically`() {
        val viewModel = SampleViewModel()
        viewModel.onEvent(SampleEvent.Increment)
        assertEquals(1, viewModel.uiState.value.counter)
    }

    @Test
    fun `onEvent SendNotice dispatches effect`() = runTest {
        val viewModel = SampleViewModel()
        viewModel.onEvent(SampleEvent.SendNotice("Hola Truequapp"))
        testDispatcher.scheduler.advanceUntilIdle()

        val effect = viewModel.uiEffect.first()
        assertEquals(SampleEffect.ShowToast("Hola Truequapp"), effect)
    }
}
