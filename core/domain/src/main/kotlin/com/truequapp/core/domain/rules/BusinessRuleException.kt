package com.truequapp.core.domain.rules

/**
 * Jerarquía de excepciones de Reglas de Negocio Invariantes de Truequapp.
 */
sealed class BusinessRuleException(
    val ruleCode: String,
    override val message: String
) : Exception("[$ruleCode] $message") {

    class ExclusivityLockException(message: String) :
        BusinessRuleException("RN-01", message)

    class ValueRatioBelowThresholdException(val actualRatio: Double, val requiredRatio: Double = 0.40) :
        BusinessRuleException("RN-02", "La oferta representa el ${(actualRatio * 100).toInt()}% del valor del artículo objetivo. El mínimo permitido es ${(requiredRatio * 100).toInt()}% para evitar spam.")

    class UnauthorizedChatAccessException(message: String) :
        BusinessRuleException("RN-03", message)

    class LedgerImbalanceException(val debitTotal: Double, val creditTotal: Double) :
        BusinessRuleException("RN-04", "Descuadre contable: Débitos ($debitTotal) no coinciden con Créditos ($creditTotal).")

    class EscrowViolationException(message: String) :
        BusinessRuleException("RN-05", message)

    class TriangularCycleAbortException(message: String) :
        BusinessRuleException("RN-06", message)

    class LateCancellationPenaltyException(val hoursRemaining: Double) :
        BusinessRuleException("RN-07", "Cancelación tardía a solo ${String.format("%.1f", hoursRemaining)} horas de la cita (límite sin sanción: 4.0 horas). Se aplicará penalización a la tasa de cumplimiento.")

    class GeofenceVerificationFailedException(val actualDistanceMeters: Double, val maxAllowedDistanceMeters: Double = 150.0) :
        BusinessRuleException("RN-08", "No se comprobó presencia física en el Punto Seguro. Distancia actual: ${actualDistanceMeters.toInt()}m (máximo permitido: ${maxAllowedDistanceMeters.toInt()}m).")

    class ProhibitedItemException(val matchedKeyword: String) :
        BusinessRuleException("RN-11", "El artículo contiene contenido prohibido por la política de seguridad ('$matchedKeyword').")

    class DualBlindNotReadyException(message: String) :
        BusinessRuleException("RN-12", message)
}
