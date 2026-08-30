package com.maxrave.simpmusic.expect.ui

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.max
import kotlin.math.min

// Hardware RenderEffect blur is supported on Android 12+ (API 31).
actual fun isHardwareBlurSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

// Unlocked for Android 10+ using Alpha Depth-of-Field Fallback.
actual fun isLyricsBlurSupported(): Boolean = true

// Fast software downscaled blur fallback for Android 10 & 11 backgrounds.
actual fun createBlurredBitmapFallback(bitmap: ImageBitmap?): ImageBitmap? {
    if (bitmap == null) return null
    return try {
        val src = bitmap.asAndroidBitmap()
        // Downsample to a small matrix (28x28) so blur executes in < 1ms
        val targetSize = 28
        val width = if (src.width > src.height) (targetSize * (src.width.toFloat() / src.height)).toInt() else targetSize
        val height = if (src.height > src.width) (targetSize * (src.height.toFloat() / src.width)).toInt() else targetSize
        val scaled = Bitmap.createScaledBitmap(src, max(8, width), max(8, height), true)
        val blurred = fastBoxBlur(scaled, radius = 3)
        blurred.asImageBitmap()
    } catch (e: Throwable) {
        bitmap
    }
}

private fun fastBoxBlur(source: Bitmap, radius: Int): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val r = max(1, radius)
    val wm = width - 1
    val hm = height - 1
    val div = r + r + 1

    val rArr = IntArray(width * height)
    val gArr = IntArray(width * height)
    val bArr = IntArray(width * height)

    for (i in pixels.indices) {
        val p = pixels[i]
        rArr[i] = (p shr 16) and 0xff
        gArr[i] = (p shr 8) and 0xff
        bArr[i] = p and 0xff
    }

    // Horizontal pass
    for (y in 0 until height) {
        val yw = y * width
        var rSum = 0
        var gSum = 0
        var bSum = 0

        for (i in -r..r) {
            val px = min(wm, max(i, 0))
            rSum += rArr[yw + px]
            gSum += gArr[yw + px]
            bSum += bArr[yw + px]
        }

        for (x in 0 until width) {
            val idx = yw + x
            pixels[idx] = (0xff shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)

            val p1 = yw + min(x + r + 1, wm)
            val p2 = yw + max(x - r, 0)

            rSum += rArr[p1] - rArr[p2]
            gSum += gArr[p1] - gArr[p2]
            bSum += bArr[p1] - bArr[p2]
        }
    }

    // Vertical pass
    val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val outPixels = IntArray(width * height)

    for (x in 0 until width) {
        var rSum = 0
        var gSum = 0
        var bSum = 0

        for (i in -r..r) {
            val py = min(hm, max(i, 0))
            val p = pixels[py * width + x]
            rSum += (p shr 16) and 0xff
            gSum += (p shr 8) and 0xff
            bSum += p and 0xff
        }

        for (y in 0 until height) {
            val idx = y * width + x
            outPixels[idx] = (0xff shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)

            val p1 = min(y + r + 1, hm) * width + x
            val p2 = max(y - r, 0) * width + x

            val pix1 = pixels[p1]
            val pix2 = pixels[p2]

            rSum += ((pix1 shr 16) and 0xff) - ((pix2 shr 16) and 0xff)
            gSum += ((pix1 shr 8) and 0xff) - ((pix2 shr 8) and 0xff)
            bSum += (pix1 and 0xff) - (pix2 and 0xff)
        }
    }

    outBitmap.setPixels(outPixels, 0, width, 0, 0, width, height)
    return outBitmap
}
