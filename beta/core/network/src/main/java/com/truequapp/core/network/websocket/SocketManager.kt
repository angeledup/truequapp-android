package com.truequapp.core.network.websocket

import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

enum class SocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Manages low-latency real-time bidirectional WebSocket communication via Socket.io.
 * Supports chat messaging, live trade updates, and real-time push notifications.
 */
@Singleton
class SocketManager @Inject constructor() {

    private var socket: Socket? = null
    private val _connectionState = MutableStateFlow(SocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<SocketConnectionState> = _connectionState.asStateFlow()

    fun connect(serverUrl: String, authToken: String? = null) {
        if (socket?.connected() == true) return

        try {
            _connectionState.value = SocketConnectionState.CONNECTING

            val options = IO.Options().apply {
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 10000
                if (!authToken.isNullOrBlank()) {
                    auth = mapOf("token" to authToken)
                }
            }

            socket = IO.socket(URI.create(serverUrl), options).apply {
                on(Socket.EVENT_CONNECT) {
                    _connectionState.value = SocketConnectionState.CONNECTED
                }
                on(Socket.EVENT_DISCONNECT) {
                    _connectionState.value = SocketConnectionState.DISCONNECTED
                }
                on(Socket.EVENT_CONNECT_ERROR) {
                    _connectionState.value = SocketConnectionState.ERROR
                }
                connect()
            }
        } catch (e: Exception) {
            _connectionState.value = SocketConnectionState.ERROR
        }
    }

    fun disconnect() {
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionState.value = SocketConnectionState.DISCONNECTED
    }

    fun joinRoom(roomId: String) {
        socket?.emit("join_room", roomId)
    }

    fun leaveRoom(roomId: String) {
        socket?.emit("leave_room", roomId)
    }

    fun emitEvent(event: String, data: Any) {
        socket?.emit(event, data)
    }

    fun listenToEvent(event: String): Flow<String> = callbackFlow {
        val listener: (Array<Any>) -> Unit = { args ->
            val payload = args.firstOrNull()?.toString() ?: ""
            trySend(payload)
        }

        socket?.on(event, listener)

        awaitClose {
            socket?.off(event, listener)
        }
    }
}
