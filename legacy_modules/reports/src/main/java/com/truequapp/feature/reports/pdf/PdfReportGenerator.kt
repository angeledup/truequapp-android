package com.truequapp.feature.reports.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.truequapp.feature.reports.mvi.LiquidityKpiData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Contrato para la generación de reportes de auditoría en formato PDF.
 */
interface PdfReportGenerator {
    fun generateLiquidityReport(kpis: LiquidityKpiData, destinationFile: File): File
}

/**
 * Implementación oficial nativa utilizando [android.graphics.pdf.PdfDocument] (CU021 / Fase 5).
 */
class AndroidPdfReportGenerator @Inject constructor() : PdfReportGenerator {

    override fun generateLiquidityReport(
        kpis: LiquidityKpiData,
        destinationFile: File
    ): File {
        // Dimensiones estándar Carta (Letter): 612 x 792 puntos a 72 DPI
        val pageWidth = 612
        val pageHeight = 792

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Configuración de pinceles de dibujo
        val headerPaint = Paint().apply {
            color = Color.rgb(5, 150, 105) // Emerald 600
            style = Paint.Style.FILL
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(209, 250, 229) // Emerald 100
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            textSize = 12f
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Slate 900
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val compliantPaint = Paint().apply {
            color = Color.rgb(5, 150, 105)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.rgb(226, 232, 240) // Slate 200
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        // 1. Encabezado con banner esmeralda
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 100f, headerPaint)
        canvas.drawText("TRUEQUAPP - AUDITORÍA DE LIQUIDEZ Y ARRANQUE EN FRÍO", 40f, 45f, titlePaint)
        canvas.drawText("Métricas de Salud Operacional y Cumplimiento SWEBOK / UV (CU021)", 40f, 65f, subtitlePaint)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        canvas.drawText("Generado el: ${dateFormat.format(Date())}", 40f, 85f, subtitlePaint)

        // 2. Resumen Ejecutivo
        var currentY = 135f
        canvas.drawText("1. ESTADO DE ARRANQUE EN FRÍO (COLD START)", 40f, currentY, boldTextPaint)

        currentY += 20f
        val healthStatus = if (kpis.isColdStartHealthy) "SALUDABLE - Cumple con todos los umbrales de liquidez" else "EN OBSERVACIÓN"
        canvas.drawText("Dictamen del Marketplace: $healthStatus", 40f, currentY, textPaint)

        currentY += 30f
        canvas.drawText("2. INDICADORES CLAVE DE RENDIMIENTO (KPIs)", 40f, currentY, boldTextPaint)

        // 3. Tabla de Indicadores
        currentY += 20f
        val rowHeight = 35f

        val kpiRows = listOf(
            Triple("Tasa Global de Aceptación (OAR)", "≥ 45.0%", "${(kpis.oarRate * 100).toInt()}%  [${if (kpis.isOarCompliant) "CUMPLE" else "ALERTA"}]"),
            Triple("Tasa de Completitud de Trueques (TCR)", "≥ 80.0%", "${(kpis.tcrRate * 100).toInt()}%  [${if (kpis.isTcrCompliant) "CUMPLE" else "ALERTA"}]"),
            Triple("Tiempo al Primer Trueque (TTFT)", "≤ 72.0 h", "${kpis.ttftHoursAverage} h  [${if (kpis.isTtftCompliant) "CUMPLE" else "ALERTA"}]"),
            Triple("Adopción de Puntos Seguros (Safe Zones)", "≥ 85.0%", "${(kpis.safeZoneAdoptionRate * 100).toInt()}%  [${if (kpis.isSafeZoneCompliant) "CUMPLE" else "ALERTA"}]"),
            Triple("Velocidad de TruequeCoins", "Dinámica", "${kpis.coinVelocityMonthly}x rotación mensual"),
            Triple("Volumen Transaccional en Custodia", "Escrow", "$${kpis.totalVolumeCoins.toInt()} TC"),
            Triple("Publicaciones Activas en Catálogo", "Inventario", "${kpis.activeListingsCount} artículos"),
            Triple("Intercambios Verificados Presenciales", "Entrega QR", "${kpis.completedTradesCount} completados")
        )

        // Dibujar filas de tabla
        kpiRows.forEachIndexed { index, (metric, target, result) ->
            val y = currentY + (index * rowHeight)
            if (index % 2 == 1) {
                val rowBgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
                canvas.drawRect(40f, y - 18f, pageWidth - 40f, y + 12f, rowBgPaint)
            }
            canvas.drawLine(40f, y + 12f, pageWidth - 40f, y + 12f, borderPaint)

            canvas.drawText(metric, 50f, y, textPaint)
            canvas.drawText(target, 320f, y, textPaint)

            val isSuccess = result.contains("CUMPLE")
            canvas.drawText(result, 420f, y, if (isSuccess) compliantPaint else boldTextPaint)
        }

        currentY += (kpiRows.size * rowHeight) + 40f

        // 4. Firmas y Garantía de Integridad
        canvas.drawText("3. MARCO DE SEGURIDAD Y PROTECCIÓN DE DATOS", 40f, currentY, boldTextPaint)
        currentY += 20f
        canvas.drawText("• Integridad de Transacciones: Partida Doble estricta (RN-04) con suma cero auditada.", 40f, currentY, textPaint)
        currentY += 16f
        canvas.drawText("• Protección de Ubicación: Geolocalización ofuscada (RN-08) y Safe Zones validadas.", 40f, currentY, textPaint)
        currentY += 16f
        canvas.drawText("• Cumplimiento Google Play: Play Integrity Standard Verdict y borrado de cuenta (CU002).", 40f, currentY, textPaint)

        currentY += 50f
        canvas.drawLine(40f, currentY, 240f, currentY, borderPaint)
        canvas.drawText("Comité de Moderación y Auditoría", 40f, currentY + 15f, textPaint)
        canvas.drawText("Truequapp Android Platform", 40f, currentY + 30f, subtitlePaint.apply { color = Color.GRAY })

        document.finishPage(page)

        // Escribir archivo en almacenamiento de destino
        FileOutputStream(destinationFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return destinationFile
    }
}
