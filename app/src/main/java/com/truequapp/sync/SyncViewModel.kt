package com.truequapp.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truequapp.core.designsystem.component.InAppNotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    val syncManager: DatabaseSyncManager
) : ViewModel() {

    val syncState: StateFlow<SyncState> = syncManager.syncState

    val inAppNotifications: StateFlow<List<InAppNotificationItem>> = syncManager.incomingOffersFlow
        .map { list ->
            list.map { item ->
                val sender = item.trueque.usuarioACorreo.substringBefore("@")
                val coins = item.ofertaA?.truequeCoins ?: 0.0
                val coinsText = if (coins > 0) " (+${coins.toInt()} TC)" else ""
                InAppNotificationItem(
                    id = item.trueque.id,
                    title = "¡Nueva oferta de $sender!",
                    description = "Propuesta recibida para tu intercambio$coinsText.",
                    time = "Pendiente",
                    proposalId = item.trueque.id
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun startPeriodicSync() {
        syncManager.startPeriodicSync(viewModelScope)
    }

    fun stopPeriodicSync() {
        syncManager.stopPeriodicSync()
    }

    fun retrySync() {
        syncManager.triggerManualSync(viewModelScope)
    }
}
