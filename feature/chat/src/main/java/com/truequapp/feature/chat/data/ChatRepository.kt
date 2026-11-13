package com.truequapp.feature.chat.data

import com.truequapp.core.database.dao.ChatMessageDao
import com.truequapp.core.database.dao.TruequeDao
import com.truequapp.core.database.dao.UserDao
import com.truequapp.core.database.entity.ChatMessageEntity
import com.truequapp.core.database.entity.TruequeConOfertas
import com.truequapp.core.network.websocket.SocketManager
import com.truequapp.feature.chat.mvi.ChatMessageItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repositorio del Módulo 4: Chat Privado Transaccional.
 * Coordina la persistencia local en SQLite Room (ChatMessageDao) y la comunicación
 * en tiempo real con WebSockets (SocketManager) bajo la regla RN-03.
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val truequeDao: TruequeDao,
    private val userDao: UserDao,
    private val socketManager: SocketManager
) {

    /**
     * Observa en tiempo real los mensajes de un trueque persistidos en SQLite Room.
     */
    fun getMessagesForProposal(proposalId: String, currentUserId: String): Flow<List<ChatMessageItem>> {
        return chatMessageDao.getMessagesForTrueque(proposalId).map { entities ->
            entities.map { entity ->
                val isMe = entity.emisorCorreo == currentUserId
                ChatMessageItem(
                    id = entity.id,
                    proposalId = entity.truequeId,
                    senderId = entity.emisorCorreo,
                    senderName = if (isMe) "Yo" else entity.emisorCorreo.substringBefore("@"),
                    content = entity.contenido,
                    timestampEpochMs = entity.fechaEnvioMs,
                    isDelivered = entity.estaEntregado,
                    isRead = entity.estaLeido,
                    isFromMe = isMe
                )
            }
        }
    }

    /**
     * Guarda el mensaje de forma local en SQLite Room y lo emite a través de SocketManager.
     */
    suspend fun sendMessage(
        proposalId: String,
        senderEmail: String,
        receiverEmail: String,
        content: String
    ): ChatMessageItem {
        val messageId = "msg_${UUID.randomUUID()}"
        val timestamp = System.currentTimeMillis()

        val entity = ChatMessageEntity(
            id = messageId,
            truequeId = proposalId,
            emisorCorreo = senderEmail,
            receptorCorreo = receiverEmail,
            contenido = content,
            fechaEnvioMs = timestamp,
            estaEntregado = true,
            estaLeido = false
        )

        chatMessageDao.insertMessage(entity)

        // Sincronizar en Localhost (si el servidor está activo) o en Vercel Cloud
        val endpoints = listOf(
            "http://10.0.2.2:8080/api/trueques/$proposalId/chat",
            "https://truequapp-pi.vercel.app/api/trueques/$proposalId/chat"
        )
        java.util.concurrent.Executors.newSingleThreadExecutor().execute {
            for (cloudUrl in endpoints) {
                try {
                    val conn = java.net.URL(cloudUrl).openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 2000
                    conn.readTimeout = 3000
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conn.doOutput = true
                    val json = org.json.JSONObject().apply {
                        put("id", messageId)
                        put("proposalId", proposalId)
                        put("emisorCorreo", senderEmail)
                        put("senderId", senderEmail)
                        put("senderName", senderEmail.substringBefore("@"))
                        put("receptorCorreo", receiverEmail)
                        put("contenido", content)
                        put("content", content)
                        put("fechaEnvioMs", timestamp)
                        put("timestampEpochMs", timestamp)
                    }
                    conn.outputStream.use { os ->
                        os.write(json.toString().toByteArray(Charsets.UTF_8))
                    }
                    val code = conn.responseCode
                    conn.disconnect()
                    if (code in 200..299) break
                } catch (_: Exception) {
                    // Intenta el siguiente endpoint (localhost -> cloud)
                }
            }
        }

        // Emisión por WebSockets en caso de estar conectado
        try {
            socketManager.emitEvent("send_message", content)
        } catch (e: Exception) {
            // Modo resiliente offline
        }

        return ChatMessageItem(
            id = messageId,
            proposalId = proposalId,
            senderId = senderEmail,
            senderName = "Yo",
            content = content,
            timestampEpochMs = timestamp,
            isDelivered = true,
            isRead = false,
            isFromMe = true
        )
    }

    /**
     * Inserta un mensaje entrante recibido vía socket o simulación en la base de datos local.
     */
    suspend fun saveIncomingMessage(
        proposalId: String,
        senderEmail: String,
        receiverEmail: String,
        content: String
    ): ChatMessageItem {
        val messageId = "msg_${UUID.randomUUID()}"
        val timestamp = System.currentTimeMillis()

        val entity = ChatMessageEntity(
            id = messageId,
            truequeId = proposalId,
            emisorCorreo = senderEmail,
            receptorCorreo = receiverEmail,
            contenido = content,
            fechaEnvioMs = timestamp,
            estaEntregado = true,
            estaLeido = true
        )

        chatMessageDao.insertMessage(entity)

        return ChatMessageItem(
            id = messageId,
            proposalId = proposalId,
            senderId = senderEmail,
            senderName = senderEmail.substringBefore("@"),
            content = content,
            timestampEpochMs = timestamp,
            isDelivered = true,
            isRead = true,
            isFromMe = false
        )
    }

    suspend fun markAsRead(proposalId: String, currentUserEmail: String) {
        chatMessageDao.markMessagesAsRead(proposalId, currentUserEmail)
    }

    suspend fun syncMessagesFromCloud(proposalId: String) {
        val endpoints = listOf(
            "http://10.0.2.2:8080/api/trueques/$proposalId/chat",
            "https://truequapp-pi.vercel.app/api/trueques/$proposalId/chat"
        )
        for (cloudUrl in endpoints) {
            try {
                val url = java.net.URL(cloudUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 1500
                conn.readTimeout = 2500
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    conn.disconnect()
                    val jsonArray = org.json.JSONArray(text)
                    val entities = mutableListOf<ChatMessageEntity>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.optJSONObject(i) ?: continue
                        val msgId = obj.optString("id", "msg_$i")
                        val emisor = obj.optString("emisorCorreo", obj.optString("senderId", ""))
                        val receptor = obj.optString("receptorCorreo", "")
                        val contenido = obj.optString("contenido", obj.optString("content", ""))
                        val fecha = obj.optLong("fechaEnvioMs", obj.optLong("timestampEpochMs", System.currentTimeMillis()))
                        entities.add(
                            ChatMessageEntity(
                                id = msgId,
                                truequeId = proposalId,
                                emisorCorreo = emisor,
                                receptorCorreo = receptor,
                                contenido = contenido,
                                fechaEnvioMs = fecha,
                                estaEntregado = true,
                                estaLeido = false
                            )
                        )
                    }
                    if (entities.isNotEmpty()) {
                        chatMessageDao.insertMessages(entities)
                    }
                    break
                } else {
                    conn.disconnect()
                }
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Sincroniza desde la nube todas las propuestas del usuario y los mensajes más recientes
     * para que aparezcan en el listado de conversaciones.
     */
    suspend fun syncConversationsFromCloud(currentUserEmail: String) {
        if (currentUserEmail.isBlank()) return
        val endpoints = listOf(
            "http://10.0.2.2:8080/api/trueques?user=${java.net.URLEncoder.encode(currentUserEmail, "UTF-8")}",
            "https://truequapp-pi.vercel.app/api/trueques?user=${java.net.URLEncoder.encode(currentUserEmail, "UTF-8")}"
        )
        for (cloudUrl in endpoints) {
            try {
                val url = java.net.URL(cloudUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 1500
                conn.readTimeout = 2500
                conn.setRequestProperty("Accept", "application/json")
                if (conn.responseCode == 200) {
                    val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    conn.disconnect()
                    val jsonArray = org.json.JSONArray(text)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.optJSONObject(i) ?: continue
                        val propId = obj.optString("id")
                        if (propId.isBlank()) continue
                        val uA = obj.optString("initiatorEmail").ifBlank { obj.optString("initiatorUserId", "") }
                        val uB = obj.optString("receiverEmail").ifBlank { obj.optString("receiverUserId", "") }
                        val statusStr = obj.optString("status", "PENDIENTE")
                        val activa = obj.optBoolean("esta_activa", true)
                        val targetId = obj.optString("targetItemId", "")
                        val offeredArray = obj.optJSONArray("offeredItemIds")
                        val offeredList = mutableListOf<String>()
                        if (offeredArray != null) {
                            for (j in 0 until offeredArray.length()) {
                                offeredList.add(offeredArray.optString(j))
                            }
                        }
                        val coins = obj.optDouble("monetaryCompensationCoins", 0.0)

                        val ofA = com.truequapp.core.database.entity.OfertaItemEntity(
                            id = "of_a_$propId",
                            usuarioCorreo = uA,
                            articulosIds = offeredList.joinToString(","),
                            truequeCoins = coins,
                            fechaCreacion = System.currentTimeMillis()
                        )
                        val ofB = com.truequapp.core.database.entity.OfertaItemEntity(
                            id = "of_b_$propId",
                            usuarioCorreo = uB,
                            articulosIds = targetId,
                            truequeCoins = 0.0,
                            fechaCreacion = System.currentTimeMillis()
                        )
                        val tr = com.truequapp.core.database.entity.TruequeEntity(
                            id = propId,
                            usuarioACorreo = uA,
                            ofertaAId = ofA.id,
                            usuarioBCorreo = uB,
                            ofertaBId = ofB.id,
                            estado = statusStr,
                            estaActiva = activa,
                            fechaCreacion = System.currentTimeMillis(),
                            fechaActualizacion = System.currentTimeMillis()
                        )
                        truequeDao.insertOfertaItem(ofA)
                        truequeDao.insertOfertaItem(ofB)
                        truequeDao.insertTrueque(tr)

                        // Sincronizar mensajes en tiempo real de esta propuesta
                        syncMessagesFromCloud(propId)
                    }
                    break
                } else {
                    conn.disconnect()
                }
            } catch (_: Exception) {
            }
        }
    }

    suspend fun getProposalById(proposalId: String): TruequeConOfertas? {
        return truequeDao.getTruequeById(proposalId)
    }

    /**
     * Consulta todas las conversaciones de trueques en las que participa el usuario para el menú principal de Chats.
     */
    fun getConversations(currentUserEmail: String): Flow<List<ChatConversationSummary>> {
        return truequeDao.getHistorialTruequesPorUsuario(currentUserEmail).map { trueques ->
            trueques
                .filter { it.trueque.estado in listOf("RESERVADO", "EN_ENTREGA", "COMPLETADO", "PENDIENTE", "CONTRAOFERTADA") }
                .map { truequeConOfertas ->
                val trueque = truequeConOfertas.trueque
                val otherEmail = if (trueque.usuarioACorreo == currentUserEmail) {
                    trueque.usuarioBCorreo
                } else {
                    trueque.usuarioACorreo
                }

                val lastEntity = chatMessageDao.getLatestMessageForTrueque(trueque.id)
                val targetItem = truequeConOfertas.ofertaB?.articulosIds.orEmpty().ifBlank { "Artículo de trueque" }

                ChatConversationSummary(
                    proposalId = trueque.id,
                    otherUserEmail = otherEmail,
                    otherUserName = otherEmail.substringBefore("@"),
                    tradeStatus = trueque.estado,
                    isTradeActive = trueque.estaActiva,
                    targetItemSummary = targetItem,
                    lastMessage = lastEntity?.contenido ?: "Conversación iniciada para este trueque",
                    lastMessageTimeMs = lastEntity?.fechaEnvioMs ?: trueque.fechaActualizacion,
                    unreadCount = 0
                )
            }
        }
    }
}

