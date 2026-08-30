package com.maxrave.simpmusic.expect.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Whether the platform supports hardware-accelerated real-time Compose `Modifier.blur` (RenderEffect on Android 12+ / Skiko on Desktop).
 */
expect fun isHardwareBlurSupported(): Boolean

/**
 * Whether Apple Music lyrics style (with typography, layout, and focus) is supported.
 * Returns true across all platforms since Android 10+ uses Alpha Depth-of-Field fallback.
 */
expect fun isLyricsBlurSupported(): Boolean

/**
 * Creates a soft blurred fallback bitmap for background rendering on platforms without hardware RenderEffect (e.g. Android 10/11).
 */
expect fun createBlurredBitmapFallback(bitmap: ImageBitmap?): ImageBitmap?

