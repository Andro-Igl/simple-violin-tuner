package com.example.simpleviolintunerad_free.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import java.util.Locale
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.simpleviolintunerad_free.viewmodel.TunerState
import com.example.simpleviolintunerad_free.viewmodel.TuningStatus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main tuner display with gauge and note.
 *
 * Shows a semicircular gauge with needle that
 * visualizes the deviation from target frequency.
 *
 * CUSTOMIZABLE:
 * - Colors via Theme or directly in functions
 * - Size via Modifier
 * - Animation parameters
 */
@Composable
fun TunerDisplay(
    state: TunerState,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = state.tuningStatus.toColor(),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "statusColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-16).dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gauge display
        TunerGauge(
            cents = state.cents.toFloat(),
            isActive = state.isActive,
            color = statusColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        // Note display
        NoteDisplay(
            note = state.note,
            isActive = state.isActive,
            color = statusColor,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Frequency display
        FrequencyDisplay(
            frequency = state.frequency,
            targetFrequency = state.targetFrequency,
            isActive = state.isActive
        )

        // Cents display
        CentsDisplay(
            cents = state.cents,
            isActive = state.isActive,
            status = state.tuningStatus
        )

        // Status text
        StatusText(status = state.tuningStatus)
    }
}

/**
 * Semicircular gauge for tuning display.
 */
@Composable
fun TunerGauge(
    cents: Float,
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Animate needle position
    val animatedCents by animateFloatAsState(
        targetValue = if (isActive) cents.coerceIn(-50f, 50f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cents"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height
        val radius = minOf(width / 2, height) * 0.85f

        // Background arc
        drawArc(
            color = Color.Gray.copy(alpha = 0.3f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = 20f, cap = StrokeCap.Round)
        )

        // Colored arc for current status
        if (isActive) {
            // Green zone in the center
            drawArc(
                color = Color(0xFF4CAF50).copy(alpha = 0.5f),
                startAngle = 265f,
                sweepAngle = 10f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                style = Stroke(width = 24f, cap = StrokeCap.Round)
            )
        }

        // Markers
        val markerCount = 11
        for (i in 0 until markerCount) {
            val angle = 180f + (180f * i / (markerCount - 1))
            val angleRad = angle * PI.toFloat() / 180f

            val innerRadius = radius * 0.7f
            val outerRadius = radius * 0.85f

            val startX = centerX + cos(angleRad) * innerRadius
            val startY = centerY + sin(angleRad) * innerRadius
            val endX = centerX + cos(angleRad) * outerRadius
            val endY = centerY + sin(angleRad) * outerRadius

            val markerColor = if (i == markerCount / 2) {
                Color(0xFF4CAF50) // Center is green
            } else {
                Color.Gray.copy(alpha = 0.5f)
            }

            drawLine(
                color = markerColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (i == markerCount / 2) 4f else 2f
            )
        }

        // Needle
        if (isActive) {
            // Needle angle: 0 Cents = 270° (top), -50 = 180°, +50 = 360°
            val needleAngle = 270f + (animatedCents / 50f * 90f)
            val needleAngleRad = needleAngle * PI.toFloat() / 180f
            val needleLength = radius * 0.65f

            val needleEndX = centerX + cos(needleAngleRad) * needleLength
            val needleEndY = centerY + sin(needleAngleRad) * needleLength

            // Needle shadow
            drawLine(
                color = Color.Black.copy(alpha = 0.3f),
                start = Offset(centerX + 2, centerY + 2),
                end = Offset(needleEndX + 2, needleEndY + 2),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // Needle
            drawLine(
                color = color,
                start = Offset(centerX, centerY),
                end = Offset(needleEndX, needleEndY),
                strokeWidth = 4f,
                cap = StrokeCap.Round
            )

            // Needle center
            drawCircle(
                color = color,
                radius = 12f,
                center = Offset(centerX, centerY)
            )
        } else {
            // Inactive: Gray dot
            drawCircle(
                color = Color.Gray,
                radius = 12f,
                center = Offset(centerX, centerY)
            )
        }
    }
}

/**
 * Large note display.
 */
@Composable
fun NoteDisplay(
    note: String,
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val displayColor by animateColorAsState(
        targetValue = if (isActive) color else Color.Gray,
        label = "noteColor"
    )

    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(displayColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = note,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = displayColor
        )
    }
}

/**
 * Frequency display with target.
 */
@Composable
fun FrequencyDisplay(
    frequency: Double,
    targetFrequency: Double,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isActive && frequency > 0) {
            Text(
                text = String.format(Locale.US, "%.1f Hz", frequency),
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (targetFrequency > 0) {
                Text(
                    text = String.format(Locale.US, "Target: %.1f Hz", targetFrequency),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            Text(
                text = "-- Hz",
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }
    }
}

/**
 * Cents deviation display.
 */
@Composable
fun CentsDisplay(
    cents: Double,
    isActive: Boolean,
    status: TuningStatus,
    modifier: Modifier = Modifier
) {
    if (!isActive) return

    val centsText = when {
        cents > 0 -> String.format(Locale.US, "+%.0f Cents", cents)
        cents < 0 -> String.format(Locale.US, "%.0f Cents", cents)
        else -> "0 Cents"
    }

    val direction = when {
        cents > 5 -> "↑ too high"
        cents < -5 -> "↓ too low"
        else -> "✓ in tune"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = centsText,
            fontSize = 14.sp,
            color = status.toColor()
        )
        Text(
            text = direction,
            fontSize = 11.sp,
            color = status.toColor().copy(alpha = 0.8f)
        )
    }
}

/**
 * Status text display.
 */
@Composable
fun StatusText(
    status: TuningStatus,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (status) {
        TuningStatus.NO_SIGNAL -> "Play a string..." to Color.Gray
        TuningStatus.IN_TUNE -> "Perfectly tuned!" to Color(0xFF4CAF50)
        TuningStatus.SLIGHTLY_SHARP -> "Slightly sharp" to Color(0xFFFFC107)
        TuningStatus.SLIGHTLY_FLAT -> "Slightly flat" to Color(0xFFFFC107)
        TuningStatus.SHARP -> "Too high - loosen the string" to Color(0xFFFF9800)
        TuningStatus.FLAT -> "Too low - tighten the string" to Color(0xFFFF9800)
        TuningStatus.VERY_SHARP -> "Way too high!" to Color(0xFFF44336)
        TuningStatus.VERY_FLAT -> "Way too low!" to Color(0xFFF44336)
    }

    Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        textAlign = TextAlign.Center,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .offset(y = (-5).dp)  // Shift 5dp up
    )
}

/**
 * Converts TuningStatus to color.
 *
 * CUSTOMIZABLE: Colors can be easily changed here.
 */
fun TuningStatus.toColor(): Color {
    return when (this) {
        TuningStatus.NO_SIGNAL -> Color.Gray
        TuningStatus.IN_TUNE -> Color(0xFF4CAF50)      // Green
        TuningStatus.SLIGHTLY_SHARP,
        TuningStatus.SLIGHTLY_FLAT -> Color(0xFFFFC107) // Yellow
        TuningStatus.SHARP,
        TuningStatus.FLAT -> Color(0xFFFF9800)          // Orange
        TuningStatus.VERY_SHARP,
        TuningStatus.VERY_FLAT -> Color(0xFFF44336)     // Red
    }
}
