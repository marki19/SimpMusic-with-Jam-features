package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A 2x2 mosaic of four images.
 */
@Composable
fun FourImagesMosaic(
    modifier: Modifier = Modifier,
    images: List<ImageData>,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    if (images.isEmpty()) return
    
    val clipped = modifier.clip(shape).aspectRatio(1f) // Ensure it's a square block
    
    if (images.size >= 4) {
        Column(clipped) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                SquareTile(images[0])
                SquareTile(images[1])
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                SquareTile(images[2])
                SquareTile(images[3])
            }
        }
    } else if (images.size >= 3) {
        Column(clipped) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                SquareTile(images[0])
                SquareTile(images[1])
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                SquareTile(images[2])
                MosaicTile(images[2], Modifier.weight(1f).aspectRatio(1f)) // Just reuse the last one if we have 3
            }
        }
    } else if (images.size >= 2) {
        Column(clipped) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                SquareTile(images[0])
                SquareTile(images[1])
            }
            Row(Modifier.fillMaxWidth().weight(1f)) {
                SquareTile(images[0])
                SquareTile(images[1])
            }
        }
    } else {
        MosaicTile(images[0], clipped)
    }
}
