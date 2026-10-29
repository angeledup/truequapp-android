package com.truequapp.feature.reports.mvi

import com.truequapp.core.common.mvi.UiEffect
import com.truequapp.core.common.mvi.UiEvent
import com.truequapp.core.common.mvi.UiState
import java.io.File

/**
 * Datos estadísticos de liquidez del marketplace y métricas de arranque en frío (CU021).
 */
data class LiquidityKpiData(
    val oarRate: Double = 0.52,          // Overall Acceptance Rate: 52% (Meta >= 45%)
    val tcrRate: Double = 0.86,          // Trade Completion Rate: 86% (Meta >= 80%)
    val ttftHoursAverage: Double = 41.5, // Time-to-First-Trade: 41.5 h (Meta <= 72 h)
    val coinVelocityMonthly: Double = 3.4,// Rotación mensual de TruequeCoins
    val safeZoneAdoptionRate: Double = 0.89, // Adopción de Puntos Seguros: 89% (Meta >= 85%)
    val activeListingsCount: Int = 124,
    val completedTradesCount: Int = 88,
    val totalVolumeCoins: Double = 42500.0
) {
    val isOarCompliant: Boolean get() = oarRate >= 0.45
    val isTcrCompliant: Boolean get() = tcrRate >= 0.80
    val isTtftCompliant: Boolean get() = ttftHoursAverage <= 72.0
    val isSafeZoneCompliant: Boolean get() = safeZoneAdoptionRate >= 0.85

    val isColdStartHealthy: Boolean
        get() = isOarCompliant && isTcrCompliant && isTtftCompliant && isSafeZoneCompliant
}

/**
 * Estado inmutable de la pantalla de Reportes e Indicadores de Liquidez.
 */
data class ReportsUiState(
    val kpis: LiquidityKpiData = LiquidityKpiData(),
    val isGeneratingPdf: Boolean = false,
    val generatedPdfFile: File? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState

/**
 * Eventos del usuario en el módulo de reportes.
 */
sealed interface ReportsUiEvent : UiEvent {
    data object RefreshKpis : ReportsUiEvent
    data class GeneratePdfReport(val destinationDir: File) : ReportsUiEvent
    data object DismissPdfPreview : ReportsUiEvent
    data object DismissError : ReportsUiEvent
}

/**
 * Efectos secundarios hacia la UI.
 */
sealed interface ReportsUiEffect : UiEffect {
    data class ShowToast(val message: String) : ReportsUiEffect
    data class PdfGenerated(val file: File) : ReportsUiEffect
}

