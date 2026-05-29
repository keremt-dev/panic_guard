package com.intellica.panicshield.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * A bespoke shield emblem inside a status ring, drawn with Canvas (no raster
 * assets). The ring softly pulses when [pulsing] so the home screen feels
 * "alive and watching" without resorting to a generic spinner.
 */
@Composable
fun ShieldEmblem(
    accent: Color,
    pulsing: Boolean,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "emblem")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )
    val ringAlpha = if (pulsing) 0.35f + 0.45f * pulse else 0.85f

    Canvas(modifier = modifier.size(208.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val ringRadius = w * 0.45f

        // Track ring (faint, always present)
        drawCircle(
            color = accent.copy(alpha = 0.12f),
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.012f),
        )
        // Active ring
        drawCircle(
            color = accent.copy(alpha = ringAlpha),
            radius = ringRadius,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.02f),
        )

        // Shield silhouette, centered, ~46% of canvas width.
        val sw = w * 0.46f
        val sh = sw * 1.18f
        val left = cx - sw / 2f
        val top = cy - sh / 2f
        val shield = Path().apply {
            moveTo(cx, top)                                   // top center
            lineTo(left, top + sh * 0.18f)                    // upper-left shoulder
            lineTo(left, top + sh * 0.55f)                    // left side
            cubicTo(
                left, top + sh * 0.82f,
                cx - sw * 0.18f, top + sh * 0.96f,
                cx, top + sh,                                 // bottom point
            )
            cubicTo(
                cx + sw * 0.18f, top + sh * 0.96f,
                left + sw, top + sh * 0.82f,
                left + sw, top + sh * 0.55f,                  // right side
            )
            lineTo(left + sw, top + sh * 0.18f)               // upper-right shoulder
            close()
        }
        drawPath(shield, color = accent.copy(alpha = 0.16f))
        drawPath(shield, color = accent, style = Stroke(width = w * 0.018f))

        // Check / pulse mark inside the shield
        val checkColor = accent
        val markPath = Path().apply {
            moveTo(cx - sw * 0.20f, cy)
            lineTo(cx - sw * 0.04f, cy + sh * 0.16f)
            lineTo(cx + sw * 0.24f, cy - sh * 0.18f)
        }
        drawPath(markPath, color = checkColor, style = Stroke(width = w * 0.022f))
    }
}
