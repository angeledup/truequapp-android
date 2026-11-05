package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus

/**
 * Estado de permisos del canal de chat privado.
 */
enum class ChatAccessPermission {
    PERMITIDO_ESCRITURA_Y_LECTURA,
    SOLO_LECTURA_HISTORIAL,
    DENEGADO
}

/**
 * Caso de uso: Restricción de Contacto Directo y Chat Transaccional (RN-03 / CU018).
 * Previene acoso y contacto fuera del contexto formal de un trueque.
 */
class ValidateDirectChatAccessUseCase {

    /**
     * Evalúa si un usuario tiene autorización para enviar mensajes en el chat de un intercambio.
     *
     * @param userId Identificador del usuario que intenta enviar mensaje o abrir el chat.
     * @param activeProposal Propuesta formal vinculada a la conversación.
     * @param currentTimeEpochMs Momento actual en milisegundos.
     * @return [ChatAccessPermission] con el nivel de acceso concedido.
     * @throws BusinessRuleException.UnauthorizedChatAccessException si no existe propuesta vinculada o el usuario no participa.
     */
    operator fun invoke(
        userId: String,
        activeProposal: TradeProposal?,
        currentTimeEpochMs: Long = System.currentTimeMillis()
    ): ChatAccessPermission {
        if (activeProposal == null) {
            throw BusinessRuleException.UnauthorizedChatAccessException(
                "Infracción RN-03: No se permite iniciar salas de chat libre sin una oferta formal vinculada."
            )
        }

        val isParticipant = userId == activeProposal.requesterUserId || userId == activeProposal.receiverUserId
        if (!isParticipant) {
            throw BusinessRuleException.UnauthorizedChatAccessException(
                "El usuario '$userId' no es participante de la propuesta '${activeProposal.id}'."
            )
        }

        return when (activeProposal.status) {
            TradeStatus.PENDIENTE,
            TradeStatus.CONTRAOFERTADA,
            TradeStatus.RESERVADO,
            TradeStatus.EN_ENTREGA -> ChatAccessPermission.PERMITIDO_ESCRITURA_Y_LECTURA

            TradeStatus.COMPLETADO,
            TradeStatus.CANCELADO,
            TradeStatus.RECHAZADO -> {
                // El chat pasa a modo solo lectura por 48 horas de auditoría (CU018)
                val tradeClosedTime = activeProposal.createdAtEpochMs // Asume timestamp de cierre
                val diffHours = (currentTimeEpochMs - tradeClosedTime) / (1000.0 * 60 * 60)
                if (diffHours <= 48.0) {
                    ChatAccessPermission.SOLO_LECTURA_HISTORIAL
                } else {
                    ChatAccessPermission.DENEGADO
                }
            }

            TradeStatus.EN_DISPUTA -> ChatAccessPermission.PERMITIDO_ESCRITURA_Y_LECTURA
            TradeStatus.EXPIRADO -> ChatAccessPermission.DENEGADO
        }
    }
}
