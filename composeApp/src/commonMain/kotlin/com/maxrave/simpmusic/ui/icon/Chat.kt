package com.maxrave.simpmusic.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val SimpIcons.Chat: ImageVector
    get() {
        if (_chat != null) {
            return _chat!!
        }
        _chat =
            ImageVector.Builder(
                name = "Chat",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ).apply {
                path(
                    fill = SolidColor(Color(0xFF000000)),
                    fillAlpha = 1.0f,
                    stroke = null,
                    strokeAlpha = 1.0f,
                    strokeLineWidth = 1.0f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter,
                    strokeLineMiter = 1.0f,
                    pathFillType = PathFillType.NonZero,
                ) {
                    moveTo(20.0f, 2.0f)
                    horizontalLineTo(4.0f)
                    curveToRelative(-1.1f, 0.0f, -1.99f, 0.9f, -1.99f, 2.0f)
                    lineTo(2.0f, 22.0f)
                    lineToRelative(4.0f, -4.0f)
                    horizontalLineToRelative(14.0f)
                    curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
                    verticalLineTo(4.0f)
                    curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
                    close()
                    moveTo(18.0f, 14.0f)
                    horizontalLineTo(6.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(12.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveTo(18.0f, 11.0f)
                    horizontalLineTo(6.0f)
                    verticalLineTo(9.0f)
                    horizontalLineToRelative(12.0f)
                    verticalLineToRelative(2.0f)
                    close()
                    moveTo(18.0f, 8.0f)
                    horizontalLineTo(6.0f)
                    verticalLineTo(6.0f)
                    horizontalLineToRelative(12.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }.build()
        return _chat!!
    }

private var _chat: ImageVector? = null
