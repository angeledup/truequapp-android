package com.truequapp.core.domain.rules

import com.truequapp.core.domain.model.TradeModality
import com.truequapp.core.domain.model.TradeProposal
import com.truequapp.core.domain.model.TradeStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RN-03: Restricción de Contacto Directo y Chat Transaccional")
class ValidateDirectChatAccessUseCaseTest {

    private lateinit var useCase: ValidateDirectChatAccessUseCase

    @BeforeEach
    fun setUp() {
        useCase = ValidateDirectChatAccessUseCase()
    }

    private fun createProposal(
        requester: String = "alice",
        receiver: String = "bob",
        status: TradeStatus = TradeStatus.PENDIENTE,
        createdAtMs: Long = 1000000000L
    ): TradeProposal {
        return TradeProposal(
            id = "prop_chat_1",
            requesterUserId = requester,
            receiverUserId = receiver,
            offeredItemIds = listOf("item_1"),
            targetItemId = "item_2",
            modality = TradeModality.DIRECTO_1_A_1,
            status = status,
            createdAtEpochMs = createdAtMs
        )
    }

    @Test
    @DisplayName("Debe lanzar UnauthorizedChatAccessException si se intenta abrir un chat sin propuesta formal")
    fun shouldThrowException_whenNoActiveProposalExists() {
        val ex = assertThrows(BusinessRuleException.UnauthorizedChatAccessException::class.java) {
            useCase(userId = "alice", activeProposal = null)
        }

        assertEquals("RN-03", ex.ruleCode)
    }

    @Test
    @DisplayName("Debe lanzar UnauthorizedChatAccessException si un usuario ajeno intenta acceder al chat")
    fun shouldThrowException_whenUserIsNotAParticipant() {
        val proposal = createProposal(requester = "alice", receiver = "bob")

        val ex = assertThrows(BusinessRuleException.UnauthorizedChatAccessException::class.java) {
            useCase(userId = "intruder_charlie", activeProposal = proposal)
        }

        assertEquals("RN-03", ex.ruleCode)
    }

    @Test
    @DisplayName("Debe permitir lectura y escritura a los participantes mientras la propuesta esté activa")
    fun shouldAllowReadAndWrite_duringActiveTrade() {
        val proposal = createProposal(requester = "alice", receiver = "bob", status = TradeStatus.RESERVADO)

        val permissionAlice = useCase(userId = "alice", activeProposal = proposal)
        val permissionBob = useCase(userId = "bob", activeProposal = proposal)

        assertEquals(ChatAccessPermission.PERMITIDO_ESCRITURA_Y_LECTURA, permissionAlice)
        assertEquals(ChatAccessPermission.PERMITIDO_ESCRITURA_Y_LECTURA, permissionBob)
    }

    @Test
    @DisplayName("Debe restringir el chat a SOLO LECTURA dentro de las 48 horas posteriores al cierre del trueque")
    fun shouldAllowReadOnly_withinFortyEightHoursOfCompletion() {
        val completedAt = 1000000000L
        val proposal = createProposal(
            requester = "alice",
            receiver = "bob",
            status = TradeStatus.COMPLETADO,
            createdAtMs = completedAt
        )
        val twentyHoursLater = completedAt + (20 * 60 * 60 * 1000L) // <= 48 horas

        val permission = useCase(
            userId = "alice",
            activeProposal = proposal,
            currentTimeEpochMs = twentyHoursLater
        )

        assertEquals(ChatAccessPermission.SOLO_LECTURA_HISTORIAL, permission)
    }
}
