package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SimpIcons.Send: ImageVector
    get() {
        if (_send != null) {
            return _send!!
        }
        _send = ImageVector.Builder(
            name = "Send",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color(0xFF2196F3)),
                fillAlpha = 1.0f,
                stroke = null,
                strokeAlpha = 1.0f,
                strokeLineWidth = 1.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 1.0f,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(3.4f, 20.4f)
                curveTo(2.7f, 20.7f, 2.0f, 20.2f, 2.1f, 19.4f)
                lineTo(3.2f, 13.5f)
                curveTo(3.3f, 13.0f, 3.7f, 12.6f, 4.2f, 12.5f)
                lineTo(12.0f, 12.0f)
                lineTo(4.2f, 11.5f)
                curveTo(3.7f, 11.4f, 3.3f, 11.0f, 3.2f, 10.5f)
                lineTo(2.1f, 4.6f)
                curveTo(2.0f, 3.8f, 2.7f, 3.3f, 3.4f, 3.6f)
                lineTo(22.2f, 11.2f)
                curveTo(22.9f, 11.5f, 22.9f, 12.5f, 22.2f, 12.8f)
                lineTo(3.4f, 20.4f)
                close()
            }
        }.build()
        return _send!!
    }

private var _send: ImageVector? = null
