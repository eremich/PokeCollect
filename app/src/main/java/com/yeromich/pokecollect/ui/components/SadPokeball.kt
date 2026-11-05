package com.yeromich.pokecollect.ui.components

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val WOBBLE_DEG = 9f
private const val WOBBLE_MS = 900

/** A little sad Pokéball for empty results. It wobbles once when it appears, like a failed catch. */
@Composable
fun SadPokeball(modifier: Modifier = Modifier, size: Dp = 76.dp) {
    val colors = MaterialTheme.colorScheme
    val top = colors.primary
    val bottom = colors.surfaceContainerHighest
    val line = colors.background
    val face = colors.onSurfaceVariant

    val context = LocalContext.current
    val reduceMotion = remember { context.motionReduced() }
    val tilt = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        tilt.animateTo(0f, keyframes {
            durationMillis = WOBBLE_MS
            -WOBBLE_DEG at WOBBLE_MS / 6
            WOBBLE_DEG at WOBBLE_MS * 2 / 6
            -WOBBLE_DEG / 2 at WOBBLE_MS * 3 / 6
            WOBBLE_DEG / 3 at WOBBLE_MS * 4 / 6
        })
    }

    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                rotationZ = tilt.value
                // Rock around the bottom, the way a ball sits on the floor.
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
    ) {
        val r = this.size.minDimension / 2f
        val c = center
        val ring = r * 0.09f

        drawArc(top, startAngle = 180f, sweepAngle = 180f, useCenter = true, topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2))
        drawArc(bottom, startAngle = 0f, sweepAngle = 180f, useCenter = true, topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2))
        drawLine(line, Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = ring)
        drawCircle(line, radius = r, center = c, style = Stroke(width = ring))
        drawCircle(line, radius = r * 0.27f, center = c)
        drawCircle(bottom, radius = r * 0.17f, center = c)

        // Sad face on the lower half: two dots and a downturned mouth.
        val eyeY = c.y + r * 0.42f
        drawCircle(face, radius = r * 0.065f, center = Offset(c.x - r * 0.26f, eyeY))
        drawCircle(face, radius = r * 0.065f, center = Offset(c.x + r * 0.26f, eyeY))
        val mouth = Path().apply {
            addArc(Rect(Offset(c.x, c.y + r * 0.78f), r * 0.2f), 200f, 140f)
        }
        drawPath(mouth, face, style = Stroke(width = r * 0.07f, cap = StrokeCap.Round))
    }
}
