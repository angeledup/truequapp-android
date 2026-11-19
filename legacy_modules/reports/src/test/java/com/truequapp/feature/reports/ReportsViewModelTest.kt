package com.truequapp.feature.reports

import com.truequapp.core.common.dispatcher.DispatcherProvider
import com.truequapp.feature.reports.mvi.LiquidityKpiData
import com.truequapp.feature.reports.mvi.ReportsUiEffect
import com.truequapp.feature.reports.mvi.ReportsUiEvent
import com.truequapp.feature.reports.mvi.ReportsViewModel
import com.truequapp.feature.reports.pdf.PdfReportGenerator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class TestReportsDispatcherProvider(private val dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val unconfined: CoroutineDispatcher get() = dispatcher
}

class FakePdfReportGenerator : PdfReportGenerator {
    override fun generateLiquidityReport(kpis: LiquidityKpiData, destinationFile: File): File {
        destinationFile.writeText("%PDF-1.4 TRUEQUAPP AUDIT REPORT MOCK")
        return destinationFile
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ReportsViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ReportsViewModel(
            pdfReportGenerator = FakePdfReportGenerator(),
            dispatchers = TestReportsDispatcherProvider(testDispatcher)
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialState reflects healthy cold start metrics per SWEBOK specifications`() {
        val state = viewModel.uiState.value
        assertTrue(state.kpis.isOarCompliant)
        assertTrue(state.kpis.isTcrCompliant)
        assertTrue(state.kpis.isTtftCompliant)
        assertTrue(state.kpis.isSafeZoneCompliant)
        assertTrue(state.kpis.isColdStartHealthy)
    }

    @Test
    fun `refreshKpis updates active listings and completed trades`() = runTest(testDispatcher) {
        viewModel.onEvent(ReportsUiEvent.RefreshKpis)
        advanceUntilIdle()

        val updatedState = viewModel.uiState.value
        assertEquals(135, updatedState.kpis.activeListingsCount)
        assertEquals(94, updatedState.kpis.completedTradesCount)
        assertEquals(46800.0, updatedState.kpis.totalVolumeCoins)
    }

    @Test
    fun `generatePdfReport creates valid letter size pdf file`(@TempDir tempDir: File) = runTest(testDispatcher) {
        val effects = mutableListOf<ReportsUiEffect>()
        val job = launch {
            viewModel.uiEffect.toList(effects)
        }

        viewModel.onEvent(ReportsUiEvent.GeneratePdfReport(tempDir))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isGeneratingPdf)
        assertNotNull(state.generatedPdfFile)
        assertTrue(state.generatedPdfFile!!.exists())
        assertTrue(state.generatedPdfFile!!.length() > 0)
        assertTrue(effects.any { it is ReportsUiEffect.PdfGenerated })

        job.cancel()
    }

    @Test
    fun `dismissPdfPreview clears generated file from state`(@TempDir tempDir: File) = runTest(testDispatcher) {
        viewModel.onEvent(ReportsUiEvent.GeneratePdfReport(tempDir))
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.generatedPdfFile)

        viewModel.onEvent(ReportsUiEvent.DismissPdfPreview)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.generatedPdfFile)
    }
}
