package com.truequapp.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    val syncManager: DatabaseSyncManager
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncManager.syncState

    fun startPeriodicSync() {
        syncManager.startPeriodicSync(viewModelScope)
    }

    fun retrySync() {
        syncManager.triggerManualSync(viewModelScope)
    }
}
