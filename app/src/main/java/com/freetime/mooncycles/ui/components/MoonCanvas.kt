package com.freetime.mooncycles.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.freetime.mooncycles.ui.theme.*
import kotlin.math.*

@Composable
fun MoonCanvas(
    phase: Double,
    size: Dp = 200.dp,
    modifier: Modifier = Modifier,
    glowEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "moon_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // Outer glow
        if (glowEnabled && phase > 0.3f) {
            val glowRadius = radius * 1.3f
            val glowIntensity = (sin(phase * PI).toFloat() * glowAlpha).coerceIn(0.05f, 0.5f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x60FFF8D0),
                        Color(0x20FFF0A0),
                        Color.Transparent
                    ),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
        }

        drawMoon(center, radius, phase.toFloat())
    }
}

private fun DrawScope.drawMoon(center: Offset, radius: Float, phase: Float) {
    val moonColor = Color(0xFFF5F0E8)
    val shadowColor = Color(0xFF0A0E1A)
    val craterColor = Color(0xFFE0D8C8)

    // Shadow of space behind
    drawCircle(color = Color(0xFF06080F), radius = radius + 2f, center = center)

    // Full moon base (always draw)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF8E8),
                Color(0xFFF0E8D0),
                Color(0xFFD8CEB8)
            ),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // Draw craters
    drawCraters(center, radius, craterColor)

    // Draw shadow to show phase
    drawPhaseShadow(center, radius, phase, shadowColor)
}

private fun DrawScope.drawCraters(center: Offset, radius: Float, craterColor: Color) {
    val craters = listOf(
        Triple(0.2f, -0.1f, 0.08f),
        Triple(-0.3f, 0.25f, 0.06f),
        Triple(0.15f, 0.35f, 0.05f),
        Triple(-0.1f, -0.3f, 0.04f),
        Triple(0.35f, 0.1f, 0.05f),
    )
    craters.forEach { (dx, dy, r) ->
        val cx = center.x + dx * radius
        val cy = center.y + dy * radius
        val cr = r * radius
        if (sqrt((cx - center.x).pow(2) + (cy - center.y).pow(2)) < radius - cr) {
            drawCircle(
                color = craterColor.copy(alpha = 0.3f),
                radius = cr,
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x20000000), Color.Transparent),
                    center = Offset(cx + cr * 0.2f, cy + cr * 0.2f),
                    radius = cr
                ),
                radius = cr,
                center = Offset(cx, cy)
            )
        }
    }
}

private fun DrawScope.drawPhaseShadow(center: Offset, radius: Float, phase: Float, shadowColor: Color) {
    // phase: 0=new, 0.25=first quarter, 0.5=full, 0.75=last quarter
    // Draw shadow overlay using clip path
    val path = Path()

    when {
        phase < 0.02f || phase > 0.98f -> {
            // New moon – fully dark
            drawCircle(color = shadowColor.copy(alpha = 0.95f), radius = radius, center = center)
        }
        phase > 0.48f && phase < 0.52f -> {
            // Full moon – no shadow
        }
        else -> {
            drawPhaseMask(center, radius, phase, shadowColor)
        }
    }
}

private fun DrawScope.drawPhaseMask(center: Offset, radius: Float, phase: Float, shadowColor: Color) {
    // Determine lit side and ellipse width
    val isWaxing = phase < 0.5f
    // normalizedPhase: 0=new, 1=full for waxing; 0=full, 1=new for waning
    val t = if (isWaxing) phase * 2f else (phase - 0.5f) * 2f

    // The shadow covers one half, and an ellipse modifies the terminator
    // ellipseX: -radius (full shadow) to +radius (no shadow on the dark side)
    val ellipseX = radius * (1f - t * 2f)  // -r..+r

    val path = Path()

    if (isWaxing) {
        // Right side lit: shadow on left half + ellipse on right
        // Shadow = left semicircle + ellipse
        path.moveTo(center.x, center.y - radius)
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        // terminator ellipse (concave on lit side)
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(center.x - ellipseX - radius * 0.05f, center.y - radius, center.x - ellipseX + radius * 0.05f, center.y + radius),
            startAngleDegrees = 90f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        path.close()
    } else {
        // Left side lit: shadow on right half
        val eX = radius * (t * 2f - 1f)
        path.moveTo(center.x, center.y - radius)
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius),
            startAngleDegrees = 270f,
            sweepAngleDegrees = -180f,
            forceMoveTo = false
        )
        path.arcTo(
            rect = androidx.compose.ui.geometry.Rect(center.x + eX - radius * 0.05f, center.y - radius, center.x + eX + radius * 0.05f, center.y + radius),
            startAngleDegrees = 270f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        path.close()
    }

    clipRect(
        left = center.x - radius - 2f,
        top = center.y - radius - 2f,
        right = center.x + radius + 2f,
        bottom = center.y + radius + 2f
    ) {
        drawPath(path = path, color = shadowColor.copy(alpha = 0.92f))
    }
}