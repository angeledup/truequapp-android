package com.truequapp.feature.reports.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truequapp.core.designsystem.theme.PaletteConfig
import com.truequapp.feature.reports.mvi.LiquidityKpiData
import com.truequapp.feature.reports.mvi.ReportsUiEffect
import com.truequapp.feature.reports.mvi.ReportsUiEvent
import com.truequapp.feature.reports.mvi.ReportsViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidityDashboardScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel.uiEffect) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ReportsUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ReportsUiEffect.PdfGenerated -> {
                    Toast.makeText(context, "PDF guardado en: ${effect.file.name}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Panel de Liquidez (CU021)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Métricas de Arranque en Frío & SWEBOK",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(ReportsUiEvent.RefreshKpis) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualizar KPIs")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Banner de Salud de Arranque en Frío
            ColdStartHealthBanner(isHealthy = state.kpis.isColdStartHealthy)

            // 2. Tarjetas de los 4 KPIs Fundamentales
            Text(
                text = "Indicadores Clave de Desempeño (KPIs)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "OAR (Aceptación)",
                    actualValue = "${(state.kpis.oarRate * 100).toInt()}%",
                    targetDescription = "Meta: ≥ 45.0%",
                    progress = state.kpis.oarRate.toFloat(),
                    isCompliant = state.kpis.isOarCompliant,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "TCR (Concreción)",
                    actualValue = "${(state.kpis.tcrRate * 100).toInt()}%",
                    targetDescription = "Meta: ≥ 80.0%",
                    progress = state.kpis.tcrRate.toFloat(),
                    isCompliant = state.kpis.isTcrCompliant,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "TTFT (1er Trueque)",
                    actualValue = "${state.kpis.ttftHoursAverage} h",
                    targetDescription = "Meta: ≤ 72.0 h",
                    progress = (1f - (state.kpis.ttftHoursAverage.toFloat() / 100f)).coerceIn(0f, 1f),
                    isCompliant = state.kpis.isTtftCompliant,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Adopción SafeZone",
                    actualValue = "${(state.kpis.safeZoneAdoptionRate * 100).toInt()}%",
                    targetDescription = "Meta: ≥ 85.0%",
                    progress = state.kpis.safeZoneAdoptionRate.toFloat(),
                    isCompliant = state.kpis.isSafeZoneCompliant,
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Métricas Secundarias de Ecosistema
            SecondaryMetricsCard(kpis = state.kpis)

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Botón de Exportación en Formato PDF
            Button(
                onClick = {
                    val pdfDir = context.getExternalFilesDir(null) ?: context.filesDir
                    viewModel.onEvent(ReportsUiEvent.GeneratePdfReport(pdfDir))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !state.isGeneratingPdf,
                colors = ButtonDefaults.buttonColors(containerColor = PaletteConfig.PrimaryDefault),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isGeneratingPdf) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generando PDF Carta...")
                } else {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exportar Informe Oficial en PDF (Carta)")
                }
            }

            // Confirmación de PDF generado
            AnimatedVisibility(visible = state.generatedPdfFile != null) {
                state.generatedPdfFile?.let { file ->
                    PdfGeneratedCard(
                        file = file,
                        onDismiss = { viewModel.onEvent(ReportsUiEvent.DismissPdfPreview) }
                    )
                }
            }
        }
    }
}

@Composable
fun ColdStartHealthBanner(isHealthy: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isHealthy) PaletteConfig.PrimaryContainerLight else PaletteConfig.ErrorContainerLight
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isHealthy) PaletteConfig.PrimaryDefault else PaletteConfig.ErrorDefault,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = if (isHealthy) "Arranque en Frío: ÓPTIMO" else "Arranque en Frío: BAJA LIQUIDEZ",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isHealthy) PaletteConfig.PrimaryDark else PaletteConfig.ErrorDefault
                )
                Text(
                    text = if (isHealthy) "La plataforma supera todos los umbrales de densidad y velocidad requeridos."
                    else "Se requiere inyección de bonos semilla de TruequeCoins para reactivar inventario.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    actualValue: String,
    targetDescription: String,
    progress: Float,
    isCompliant: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = actualValue,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = if (isCompliant) PaletteConfig.PrimaryDefault else PaletteConfig.ErrorDefault
            )

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (isCompliant) PaletteConfig.PrimaryDefault else PaletteConfig.ErrorDefault,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = targetDescription,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SecondaryMetricsCard(kpis: LiquidityKpiData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Métricas de Circulación de Moneda",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Rotación Mensual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${kpis.coinVelocityMonthly}x", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PaletteConfig.SecondaryDefault)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("En Custodia (Escrow)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$${kpis.totalVolumeCoins.toInt()} TC", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Trueques Concretados", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${kpis.completedTradesCount}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = PaletteConfig.PrimaryDefault)
                }
            }
        }
    }
}

@Composable
fun PdfGeneratedCard(
    file: File,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PaletteConfig.PrimaryContainerLight
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Description, contentDescription = null, tint = PaletteConfig.PrimaryDefault)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Reporte PDF Generado",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${file.name} (${file.length() / 1024} KB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }
    }
}

