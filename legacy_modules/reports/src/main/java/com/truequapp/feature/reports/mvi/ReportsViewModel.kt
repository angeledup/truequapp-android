package com.truequapp.feature.reports.mvi

import androidx.lifecycle.viewModelScope
import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.core.common.mvi.BaseViewModel
import com.truequapp.feature.reports.pdf.PdfReportGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel del Panel de Liquidez, Arranque en Frío y Exportación PDF (CU021 / Fase 5).
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val pdfReportGenerator: PdfReportGenerator,
    private val dispatchers: DispatcherProvider
) : BaseViewModel<ReportsUiState, ReportsUiEvent, ReportsUiEffect>(ReportsUiState()) {

    override fun onEvent(event: ReportsUiEvent) {
        when (event) {
            is ReportsUiEvent.RefreshKpis -> refreshKpis()
            is ReportsUiEvent.GeneratePdfReport -> generatePdf(event.destinationDir)
            is ReportsUiEvent.DismissPdfPreview -> setState { copy(generatedPdfFile = null) }
            is ReportsUiEvent.DismissError -> setState { copy(errorMessage = null) }
        }
    }

    private fun refreshKpis() {
        setState { copy(isLoading = true) }
        viewModelScope.launch(dispatchers.io) {
            val updated = currentState.kpis.copy(
                activeListingsCount = 135,
                completedTradesCount = 94,
                totalVolumeCoins = 46800.0,
                oarRate = 0.54,
                tcrRate = 0.88,
                ttftHoursAverage = 38.0
            )
            setState {
                copy(
                    kpis = updated,
                    isLoading = false
                )
            }
            sendEffect(ReportsUiEffect.ShowToast("Indicadores de liquidez actualizados"))
        }
    }

    private fun generatePdf(destinationDir: File) {
        setState { copy(isGeneratingPdf = true) }
        viewModelScope.launch(dispatchers.io) {
            try {
                if (!destinationDir.exists()) {
                    destinationDir.mkdirs()
                }
                val outputFile = File(destinationDir, "truequapp_informe_liquidez_${System.currentTimeMillis()}.pdf")
                val generated = pdfReportGenerator.generateLiquidityReport(currentState.kpis, outputFile)

                setState {
                    copy(
                        isGeneratingPdf = false,
                        generatedPdfFile = generated
                    )
                }
                sendEffect(ReportsUiEffect.PdfGenerated(generated))
                sendEffect(ReportsUiEffect.ShowToast("Reporte PDF generado exitosamente"))
            } catch (e: Exception) {
                setState {
                    copy(
                        isGeneratingPdf = false,
                        errorMessage = "Error al generar el reporte PDF: ${e.message}"
                    )
                }
            }
        }
    }
}
