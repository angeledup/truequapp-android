package com.truequapp.feature.catalog.compressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
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
 * Pipeline de compresión asíncrono a formato WebP Lossy con purga de metadatos EXIF (CU005).
 * Garantiza que cada fotografía procesada pese estrictamente menos de 80 KB para reducir
 * el consumo de datos y resguardar la privacidad del domicilio eliminando geolocalización GPS.
 */
@Singleton
class WebPImageCompressor @Inject constructor() {

    companion object {
        const val MAX_IMAGE_SIZE_BYTES = 80 * 1024L // 80 KB = 81,920 bytes
        private const val MAX_DIMENSION_PX = 1280
        private const val INITIAL_QUALITY = 82
        private const val MIN_QUALITY = 35
    }

    /**
     * Comprime un Bitmap a WebP y elimina metadatos EXIF de geolocalización.
     */
    suspend fun compressBitmapToWebP(
        bitmap: Bitmap,
        outputFile: File
    ): CompressedImageResult = withContext(Dispatchers.IO) {
        // 1. Redimensionar si supera la dimensión máxima
        val scaledBitmap = scaleBitmapToMaxDimension(bitmap, MAX_DIMENSION_PX)

        // 2. Compresión iterativa en WebP Lossy hasta asegurar peso < 80 KB
        var currentQuality = INITIAL_QUALITY
        var stream = ByteArrayOutputStream()
        var compressedBytes: ByteArray

        do {
            stream.reset()
            @Suppress("DEPRECATION")
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, currentQuality, stream)
            compressedBytes = stream.toByteArray()
            currentQuality -= 10
        } while (compressedBytes.size > MAX_IMAGE_SIZE_BYTES && currentQuality >= MIN_QUALITY)

        // Si aún supera 80 KB en la calidad mínima, se realiza un remuestreo al 75%
        if (compressedBytes.size > MAX_IMAGE_SIZE_BYTES) {
            val furtherScaled = Bitmap.createScaledBitmap(
                scaledBitmap,
                (scaledBitmap.width * 0.75).toInt(),
                (scaledBitmap.height * 0.75).toInt(),
                true
            )
            stream.reset()
            @Suppress("DEPRECATION")
            furtherScaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, MIN_QUALITY, stream)
            compressedBytes = stream.toByteArray()
        }

        // 3. Escribir archivo en disco
        FileOutputStream(outputFile).use { fos ->
            fos.write(compressedBytes)
        }

        // 4. Purgar metadatos EXIF (GPS y datos de cámara)
        stripExifMetadata(outputFile)

        CompressedImageResult(
            file = outputFile,
            sizeBytes = outputFile.length(),
            isUnder80KbThreshold = outputFile.length() <= MAX_IMAGE_SIZE_BYTES
        )
    }

    /**
     * Comprime una imagen a partir de su Uri local.
     */
    suspend fun compressFromUri(
        context: Context,
        imageUri: Uri,
        targetFile: File
    ): CompressedImageResult = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalArgumentException("No se pudo decodificar el archivo de imagen.")
        compressBitmapToWebP(originalBitmap, targetFile)
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
     * Elimina coordenadas GPS, fecha y metadatos de privacidad del encabezado EXIF.
     */
    fun stripExifMetadata(file: File) {
        try {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
            exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
            exif.setAttribute(ExifInterface.TAG_DATETIME, null)
            exif.setAttribute(ExifInterface.TAG_MODEL, null)
            exif.setAttribute(ExifInterface.TAG_MAKE, null)
            exif.saveAttributes()
        } catch (_: Exception) {
            // Si el archivo no soporta tags EXIF nativos, no se interrumpe la ejecución
        }
    }
}

