package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap

actual fun isHardwareBlurSupported(): Boolean = true

actual fun isLyricsBlurSupported(): Boolean = true

actual fun createBlurredBitmapFallback(bitmap: ImageBitmap?): ImageBitmap? = bitmap
