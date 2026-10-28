package com.truequapp.core.common.result

/**
 * Generic wrapper representing asynchronous data operations and state.
 */
sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Error(val throwable: Throwable, val message: String? = throwable.localizedMessage) : AppResult<Nothing>
    data object Loading : AppResult<Nothing>
}
