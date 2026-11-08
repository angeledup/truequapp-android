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

