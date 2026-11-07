package com.truequapp.feature.chat.data

/**
 * Resumen de una conversación de trueque para la lista principal de mensajes (Módulo 4).
 */
data class ChatConversationSummary(
    val proposalId: String,
    val otherUserEmail: String,
    val otherUserName: String,
    val tradeStatus: String,
    val isTradeActive: Boolean,
    val targetItemSummary: String,
    val lastMessage: String,
    val lastMessageTimeMs: Long,
    val unreadCount: Int = 0
)

