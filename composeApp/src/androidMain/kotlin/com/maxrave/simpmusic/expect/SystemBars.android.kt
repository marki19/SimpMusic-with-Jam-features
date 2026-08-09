package com.maxrave.simpmusic.expect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.maxrave.simpmusic.extension.findActivity

@Composable
actual fun SetSystemBarsVisibility(visible: Boolean) {
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(visible, view) {
        val activity = context.findActivity()
        val window = (view.parent as? DialogWindowProvider)?.window ?: activity.window
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.apply {
            if (visible) {
                show(WindowInsetsCompat.Type.systemBars())
            } else {
                hide(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}
