package com.truequapp.core.common.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI (Model-View-Intent) unidirectional data flow pattern.
 *
 * @param S The concrete type of [UiState] managed by this ViewModel.
 * @param E The concrete type of [UiEvent] consumed by this ViewModel.
 * @param F The concrete type of [UiEffect] dispatched to the UI.
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _uiEffect = Channel<F>(Channel.BUFFERED)
    val uiEffect: Flow<F> = _uiEffect.receiveAsFlow()

    /**
     * Entry point for processing all UI actions / events.
     */
    abstract fun onEvent(event: E)

    /**
     * Atomically mutates the current UI state using a reducer lambda.
     */
    protected fun setState(reducer: S.() -> S) {
        _uiState.update(reducer)
    }

    /**
     * Dispatches a single-shot effect (e.g. snackbar message, navigation action).
     */
    protected fun sendEffect(effect: F) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    /**
     * Read-only snapshot of the current state.
     */
    protected val currentState: S
        get() = _uiState.value
}
