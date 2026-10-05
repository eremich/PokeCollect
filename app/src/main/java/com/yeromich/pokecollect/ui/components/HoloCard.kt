package com.yeromich.pokecollect.ui.components

import android.content.Context
import android.os.Build
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.yeromich.pokecollect.domain.model.Card
import kotlinx.coroutines.launch

// Real TCG cards have corners of roughly 4.5% of their width.
val CardShape = RoundedCornerShape(percent = 5)

private const val MAX_ROTATION_DEG = 12f
private const val TILT_GAIN = 2.2f
private const val TILT_SMOOTHING = 0.15f
// The neutral pose slowly follows how the phone is held, so the foil re-centres when you stop moving.
private const val TILT_RECENTER = 0.01f
private const val DRAG_SENSITIVITY = 2f
private const val GRID_SHIMMER_MS = 4_000

private val foilColors = listOf(
    Color.Transparent,
    Color(0xFFFF4FD8),
    Color(0xFF4FE3FF),
    Color(0xFFFFF27A),
    Color(0xFF7CFF9B),
    Color(0xFFB57BFF),
    Color.Transparent
)

fun isFoilVariant(variant: String): Boolean =
    variant.contains("holo", ignoreCase = true) || variant.contains("reverse", ignoreCase = true)

fun Context.motionReduced(): Boolean =
    Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * Card art that tilts with the phone and under the finger. Foil cards get a moving rainbow sheen
 * and a glare spot that follow the tilt, like light catching a real holo card.
 */
@Composable
fun HoloCard(card: Card, foil: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reduceMotion = remember { context.motionReduced() }
    val device by rememberDeviceTilt(enabled = !reduceMotion)
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .pointerInput(reduceMotion) {
                if (reduceMotion) return@pointerInput
                detectDragGestures(
                    onDragEnd = {
                        scope.launch { drag.animateTo(Offset.Zero, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                    },
                    onDragCancel = { scope.launch { drag.animateTo(Offset.Zero) } }
                ) { change, amount ->
                    change.consume()
                    val next = drag.value + Offset(
                        amount.x / size.width * DRAG_SENSITIVITY,
                        amount.y / size.height * DRAG_SENSITIVITY
                    )
                    scope.launch { drag.snapTo(Offset(next.x.coerceIn(-1f, 1f), next.y.coerceIn(-1f, 1f))) }
                }
            }
            .graphicsLayer {
                val tilt = (device + drag.value).clamp()
                rotationY = tilt.x * MAX_ROTATION_DEG
                rotationX = -tilt.y * MAX_ROTATION_DEG
                cameraDistance = 14f * density
            }
            .clip(CardShape)
            .foil(enabled = foil) { (device + drag.value).clamp() }
    ) {
        AsyncImage(
            // The small image is already in memory from the grid, so it fills the frame instantly
            // while the large one loads and crossfades over it.
            model = ImageRequest.Builder(context)
                .data(card.imageLarge)
                .placeholderMemoryCacheKey(card.imageSmall)
                .crossfade(true)
                .build(),
            contentDescription = card.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )
    }
}

/** Slow, self-running sheen for foil cards in grids, where per-card sensors would be wasteful. */
@Composable
fun Modifier.gridShimmer(enabled: Boolean): Modifier {
    if (!enabled) return this
    val context = LocalContext.current
    val reduceMotion = remember { context.motionReduced() }
    if (reduceMotion) return foil(enabled = true, strength = 0.6f) { Offset.Zero }
    val phase by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(GRID_SHIMMER_MS, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmerPhase"
    )
    return foil(enabled = true, strength = 0.6f) { Offset(phase, phase * 0.5f) }
}

private fun Offset.clamp() = Offset(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f))

private fun Modifier.foil(enabled: Boolean, strength: Float = 1f, tilt: () -> Offset): Modifier {
    if (!enabled) return this
    // Colour-dodge needs API 29; older phones get a plain, fainter overlay instead.
    val dodge = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val sheenAlpha = (if (dodge) 0.38f else 0.14f) * strength
    val glareAlpha = 0.16f * strength
    return graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val t = tilt()
            val w = size.width
            val h = size.height
            val shift = Offset(t.x * w, t.y * h * 0.6f)
            drawRect(
                brush = Brush.linearGradient(
                    colors = foilColors,
                    start = Offset(-w * 0.2f, h * 0.1f) + shift,
                    end = Offset(w * 0.6f, h * 0.7f) + shift,
                    tileMode = TileMode.Mirror
                ),
                alpha = sheenAlpha,
                blendMode = if (dodge) BlendMode.ColorDodge else BlendMode.SrcOver
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = Offset(w * (0.5f - t.x * 0.6f), h * (0.5f - t.y * 0.6f)),
                    radius = w * 0.75f
                ),
                alpha = glareAlpha,
                blendMode = BlendMode.Plus
            )
        }
}

@Composable
private fun rememberDeviceTilt(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }
    LifecycleResumeEffect(enabled) {
        val manager = context.getSystemService(SensorManager::class.java)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (!enabled || manager == null || sensor == null) return@LifecycleResumeEffect onPauseOrDispose {}

        var baseX = Float.NaN
        var baseY = Float.NaN
        var x = 0f
        var y = 0f
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val gx = event.values[0] / SensorManager.GRAVITY_EARTH
                val gy = event.values[1] / SensorManager.GRAVITY_EARTH
                if (baseX.isNaN()) {
                    baseX = gx
                    baseY = gy
                }
                baseX += (gx - baseX) * TILT_RECENTER
                baseY += (gy - baseY) * TILT_RECENTER
                x += (-(gx - baseX) * TILT_GAIN - x) * TILT_SMOOTHING
                y += ((gy - baseY) * TILT_GAIN - y) * TILT_SMOOTHING
                tilt.value = Offset(x, y).clamp()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onPauseOrDispose {
            manager.unregisterListener(listener)
            tilt.value = Offset.Zero
        }
    }
    return tilt
}
