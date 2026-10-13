package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveformVisualizer(
    amplitudes: List<Int>,
    progressRatio: Float = 0f,
    isLive: Boolean = false,
    onSeekRatio: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    activeBarColor: Color = MaterialTheme.colorScheme.secondary,
    inactiveBarColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_pulse")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val displayAmps = if (amplitudes.isEmpty()) {
        listOf(20, 35, 50, 75, 90, 60, 40, 80, 95, 70, 40, 65, 85, 50, 30, 60, 75, 40, 20, 50)
    } else amplitudes

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(onSeekRatio) {
                if (onSeekRatio != null) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekRatio(ratio)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val barCount = displayAmps.size
        val barGap = 4f
        val totalGaps = (barCount - 1) * barGap
        val barWidth = maxOf(4f, (width - totalGaps) / barCount)

        // Draw center baseline for live waveform feel
        val centerY = height / 2f
        drawLine(
            color = inactiveBarColor.copy(alpha = 0.3f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 2f
        )

        displayAmps.forEachIndexed { index, amp ->
            val isLatestLiveBar = isLive && (index >= barCount - 4)
            val scale = if (isLatestLiveBar) pulseAnim else 1.0f
            val rawNormalized = (amp.coerceIn(8, 100) / 100f)
            val barHeight = (height * rawNormalized * scale).coerceAtMost(height)

            val x = index * (barWidth + barGap)
            val y = (height - barHeight) / 2f

            val isPlayed = (index.toFloat() / barCount) <= progressRatio
            val currentColor = when {
                isLive -> if (isLatestLiveBar) barColor else barColor.copy(alpha = 0.85f)
                isPlayed -> activeBarColor
                else -> inactiveBarColor
            }

            drawRoundRect(
                color = currentColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )

            // Draw top peak dot on latest live bars for real-time visual feedback
            if (isLive && isLatestLiveBar) {
                drawCircle(
                    color = barColor,
                    radius = barWidth / 1.8f,
                    center = Offset(x + barWidth / 2f, y - 4f)
                )
            }
        }
    }
}
