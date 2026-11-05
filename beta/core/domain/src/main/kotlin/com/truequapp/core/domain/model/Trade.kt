package com.truequapp.core.domain.model

/**
 * Modalidades de intercambio soportadas según la Sección 4 del documento base de la UV.
 */
enum class TradeModality {
    DIRECTO_1_A_1,         // Un artículo por otro artículo
    COMPUESTO_N_A_1,       // Varios artículos propios por uno ajeno
    HIBRIDO_CON_CREDITOS,  // Artículos + diferencial compensatorio en TruequeCoins
    PURO_TRUEQUECOINS,     // 100% adquirido mediante créditos en custodia Escrow
    TRIANGULAR_ALGORITMICO // Ciclo cerrado de 3 participantes (A -> B -> C -> A)
}

/**
 * Estados transaccionales de una propuesta de trueque (Máquina de Estados de la UV).
 */
enum class TradeStatus {
    PENDIENTE,         // Oferta enviada, en espera de respuesta (vigencia TTL 72h)
    CONTRAOFERTADA,    // El receptor modificó artículos o diferencial y turnó la decisión al proponente
    RESERVADO,         // Oferta aceptada: bloqueo atómico ACID de ítems y retención de Escrow
    EN_ENTREGA,        // Cita acordada en Safe Exchange Zone
    COMPLETADO,        // Confirmación presencial exitosa por código QR
    CANCELADO,         // Cancelada por cualquiera de las partes antes de la entrega
    RECHAZADO,         // Rechazo formal por parte del receptor
    EXPIRADO,          // Vencido por TTL de 72 horas sin respuesta
    EN_DISPUTA         // Incidencia reportada (inasistencia o producto no coincidente)
}

/**
 * Entidad que modela una propuesta de trueque formal entre dos partes.
 */
data class TradeProposal(
    val id: String,
    val requesterUserId: String,
    val receiverUserId: String,
    val offeredItemIds: List<String>,
    val targetItemId: String,
    val compensatoryTruequeCoins: Double = 0.0,
    val modality: TradeModality,
    val status: TradeStatus = TradeStatus.PENDIENTE,
    val meetingId: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val expiresAtEpochMs: Long = System.currentTimeMillis() + (72 * 60 * 60 * 1000L), // TTL de 72 horas
    val parentProposalId: String? = null // Referencia si proviene de una contraoferta
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() > expiresAtEpochMs && status == TradeStatus.PENDIENTE

    val isChatAllowed: Boolean
        get() = status in listOf(
            TradeStatus.PENDIENTE,
            TradeStatus.CONTRAOFERTADA,
            TradeStatus.RESERVADO,
            TradeStatus.EN_ENTREGA
        )

    val isCancellable: Boolean
        get() = status in listOf(
            TradeStatus.PENDIENTE,
            TradeStatus.CONTRAOFERTADA,
            TradeStatus.RESERVADO,
            TradeStatus.EN_ENTREGA
        )

    val requesterId: String
        get() = requesterUserId

    val receiverId: String
        get() = receiverUserId

    val compensatoryCoins: Double
        get() = compensatoryTruequeCoins

    val expiresAtUtc: Long
        get() = expiresAtEpochMs

    /**
     * RN-02: Verifica si la suma de los artículos ofrecidos más los TruequeCoins compensatorios
     * alcanza al menos el 40% del valor estimado del artículo objetivo.
     */
    fun meetsMinimumValueRatio(targetItemValue: Double, offeredTotalValue: Double): Boolean {
        if (targetItemValue <= 0.0) return false
        val totalOffered = offeredTotalValue + compensatoryTruequeCoins
        return (totalOffered / targetItemValue) >= 0.40 // RN-02: Mínimo 40%
    }

    /**
     * Constructor secundario compatible con la firma canónica de FASE 0.
     */
    constructor(
        id: String,
        requesterId: String,
        receiverId: String,
        offeredItemIds: List<String>,
        targetItemId: String,
        compensatoryCoins: Double,
        status: TradeStatus,
        expiresAtUtc: Long
    ) : this(
        id = id,
        requesterUserId = requesterId,
        receiverUserId = receiverId,
        offeredItemIds = offeredItemIds,
        targetItemId = targetItemId,
        compensatoryTruequeCoins = compensatoryCoins,
        modality = if (compensatoryCoins > 0.0 && offeredItemIds.isNotEmpty()) {
            TradeModality.HIBRIDO_CON_CREDITOS
        } else if (compensatoryCoins > 0.0) {
            TradeModality.PURO_TRUEQUECOINS
        } else if (offeredItemIds.size > 1) {
            TradeModality.COMPUESTO_N_A_1
        } else {
            TradeModality.DIRECTO_1_A_1
        },
        status = status,
        expiresAtEpochMs = expiresAtUtc
    )
}
