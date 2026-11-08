package com.truequapp.feature.catalog.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resultado de la compresión y sanitización de una imagen para inventario.
 */
data class CompressedImageResult(
    val file: File,
    val sizeBytes: Long,
    val isUnder80KbThreshold: Boolean,
    val mimeType: String = "image/webp"
)

/**
 * Pipeline de compresión nativo asíncrono a formato WebP Lossy con purga de metadatos EXIF (CU005).
 * Ejecuta en Coroutines (Dispatchers.Default):
 * 1. Decodifica el Bitmap aplicando sub-muestreo inteligente (inSampleSize) para no agotar la memoria heap.
 * 2. Remueve metadatos EXIF (coordenadas GPS de la cámara y datos de captura).
 * 3. Comprime a formato WebP lossy con ajuste dinámico de calidad (80 -> 70 -> 60 ...) hasta asegurar un peso estricto < 80 KB.
 * 4. Admite procesamiento por lotes para hasta 6 fotografías simultáneas.
 */
@Singleton
class WebPImageCompressor @Inject constructor() {

    companion object {
        const val MAX_IMAGE_SIZE_BYTES = 80 * 1024L // 80 KB = 81,920 bytes
        const val MAX_DIMENSION_PX = 1280
        const val INITIAL_QUALITY = 80
        const val MIN_QUALITY = 30
        const val QUALITY_STEP = 10
        const val MAX_ALLOWED_PHOTOS = 6
    }

    /**
     * Decodifica un Bitmap desde un Uri aplicando sub-muestreo inteligente (inSampleSize)
     * para prevenir OutOfMemoryError en dispositivos con memoria heap limitada.
     */
    fun decodeSampledBitmapFromUri(
        ctx: Context,
        imageUri: Uri,
        reqWidth: Int = MAX_DIMENSION_PX,
        reqHeight: Int = MAX_DIMENSION_PX
    ): Bitmap? {
        // Paso 1: Decodificar solo límites (inJustDecodeBounds) sin instanciar mapa de bits en heap
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        try {
            ctx.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (_: Exception) {
            return null
        }

        if (options.outWidth <= 0 || options.outHeight <= 0) return null

        // Paso 2: Calcular inSampleSize óptimo (potencias de 2)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        // Paso 3: Decodificar con sub-muestreo y configuración de memoria eficiente
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        return try {
            ctx.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Calcula la potencia de 2 para inSampleSize basado en dimensiones objetivo.
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight || (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    /**
     * Comprime un Bitmap a formato WebP Lossy con ajuste dinámico de calidad
     * y elimina metadatos EXIF de geolocalización.
     */
    suspend fun compressBitmapToWebP(
        bitmap: Bitmap,
        outputFile: File
    ): CompressedImageResult = withContext(Dispatchers.Default) {
        // 1. Redimensionar si supera la dimensión máxima permitida
        val scaledBitmap = scaleBitmapToMaxDimension(bitmap, MAX_DIMENSION_PX)

        // 2. Compresión iterativa en WebP Lossy (ej. calidad 80 -> 70 -> 60 ...) hasta asegurar < 80 KB
        var currentQuality = INITIAL_QUALITY
        val stream = ByteArrayOutputStream()
        var compressedBytes: ByteArray
        val compressFormat = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        do {
            stream.reset()
            scaledBitmap.compress(compressFormat, currentQuality, stream)
            compressedBytes = stream.toByteArray()
            currentQuality -= QUALITY_STEP
        } while (compressedBytes.size > MAX_IMAGE_SIZE_BYTES && currentQuality >= MIN_QUALITY)

        // 3. Si aún supera 80 KB en la calidad mínima, se realiza un remuestreo adaptativo progresivo
        if (compressedBytes.size > MAX_IMAGE_SIZE_BYTES) {
            var factor = 0.8
            while (compressedBytes.size > MAX_IMAGE_SIZE_BYTES && factor >= 0.35) {
                val newW = (scaledBitmap.width * factor).toInt()
                val newH = (scaledBitmap.height * factor).toInt()
                if (newW > 120 && newH > 120) {
                    val furtherScaled = Bitmap.createScaledBitmap(scaledBitmap, newW, newH, true)
                    stream.reset()
                    furtherScaled.compress(compressFormat, MIN_QUALITY, stream)
                    compressedBytes = stream.toByteArray()
                }
                factor -= 0.15
            }
        }

        // 4. Escribir archivo físico en disco
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { fos ->
            fos.write(compressedBytes)
        }

        // 5. Purgar metadatos EXIF (coordenadas GPS de la cámara y datos de captura)
        stripExifMetadata(outputFile)

        CompressedImageResult(
            file = outputFile,
            sizeBytes = outputFile.length(),
            isUnder80KbThreshold = outputFile.length() <= MAX_IMAGE_SIZE_BYTES
        )
    }

    /**
     * Comprime una imagen a partir de su Uri local aplicando sub-muestreo y WebP lossy.
     */
    suspend fun compressFromUri(
        ctx: Context,
        imageUri: Uri,
        targetFile: File
    ): CompressedImageResult = withContext(Dispatchers.Default) {
        val sampledBitmap = decodeSampledBitmapFromUri(ctx, imageUri)
            ?: throw IllegalArgumentException("No se pudo decodificar el archivo de imagen desde $imageUri.")
        compressBitmapToWebP(sampledBitmap, targetFile)
    }

    /**
     * Comprime un lote de hasta 6 imágenes en paralelo/secuencia a formato WebP < 80 KB.
     */
    suspend fun compressMultipleUris(
        ctx: Context,
        uris: List<Uri>,
        outputDir: File = File(ctx.cacheDir, "catalog_webp_images")
    ): List<CompressedImageResult> = withContext(Dispatchers.Default) {
        outputDir.mkdirs()
        val boundedUris = uris.take(MAX_ALLOWED_PHOTOS)
        val results = mutableListOf<CompressedImageResult>()

        for ((index, uri) in boundedUris.withIndex()) {
            val targetFile = File(outputDir, "item_img_${System.currentTimeMillis()}_$index.webp")
            val result = compressFromUri(ctx, uri, targetFile)
            results.add(result)
        }
        results
    }

    private fun scaleBitmapToMaxDimension(original: Bitmap, maxDim: Int): Bitmap {
        val width = original.width
        val height = original.height

        if (width <= maxDim && height <= maxDim) return original

        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDim
            targetHeight = (maxDim / ratio).toInt()
        } else {
            targetHeight = maxDim
            targetWidth = (maxDim * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(original, targetWidth, targetHeight, true)
    }

    /**
     * Remueve metadatos EXIF (coordenadas GPS de la cámara y datos de captura).
     */
    fun stripExifMetadata(file: File) {
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_PROCESSING_METHOD, null)
            exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
            exif.setAttribute(ExifInterface.TAG_DATETIME, null)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, null)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, null)
            exif.setAttribute(ExifInterface.TAG_MODEL, null)
            exif.setAttribute(ExifInterface.TAG_MAKE, null)
            exif.setAttribute(ExifInterface.TAG_SOFTWARE, null)
            exif.saveAttributes()
        } catch (_: Exception) {
            // Si el archivo no soporta tags EXIF nativos, continúa sin interrumpir
        }
    }
}
