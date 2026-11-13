package com.truequapp.feature.catalog

import android.graphics.BitmapFactory
import com.truequapp.feature.catalog.compressor.WebPImageCompressor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class WebPImageCompressorTest {

    private val compressor = WebPImageCompressor()

    @Test
    @DisplayName("Compresor WebP: Umbral estricto < 80 KB y límite de 6 fotos de Google Play")
    fun shouldHaveCorrectConstants() {
        assertEquals(80 * 1024L, WebPImageCompressor.MAX_IMAGE_SIZE_BYTES)
        assertEquals(81920L, WebPImageCompressor.MAX_IMAGE_SIZE_BYTES)
        assertEquals(1280, WebPImageCompressor.MAX_DIMENSION_PX)
        assertEquals(6, WebPImageCompressor.MAX_ALLOWED_PHOTOS)
    }

    @Test
    @DisplayName("Sub-muestreo inteligente: InSampleSize calcula potencias de 2 para no agotar la memoria heap")
    fun calculateInSampleSize_computesCorrectPowerOfTwo() {
        val options = BitmapFactory.Options().apply {
            outWidth = 3840
            outHeight = 2160
        }

        val inSampleSize = compressor.calculateInSampleSize(options, reqWidth = 1280, reqHeight = 1280)
        // 3840x2160 reducido a la mitad es 1920x1080 (> 1280), siguiente mitad es 960x540 (<= 1280), por lo que inSampleSize = 2 o 4
        assertTrue(inSampleSize >= 2)
        assertEquals(2, inSampleSize)
    }

    @Test
    @DisplayName("Sub-muestreo inteligente: Dimensiones menores o iguales al objetivo retornan inSampleSize = 1")
    fun calculateInSampleSize_returnsOneForSmallImages() {
        val options = BitmapFactory.Options().apply {
            outWidth = 800
            outHeight = 600
        }

        val inSampleSize = compressor.calculateInSampleSize(options, reqWidth = 1280, reqHeight = 1280)
        assertEquals(1, inSampleSize)
    }
}

