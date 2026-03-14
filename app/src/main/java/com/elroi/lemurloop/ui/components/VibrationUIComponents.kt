package com.elroi.lemurloop.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elroi.lemurloop.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VibrationWaveform(
    pattern: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    isAnimated: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animationScale by if (isAnimated) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(150, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val barThickness = 8.dp.toPx()
        val cornerRadius = 4.dp.toPx()

        when (pattern) {
            "RAPID_PULSE" -> {
                // Two medium pulses
                val pulseWidth = width * 0.35f
                val gap = width * 0.1f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(width * 0.1f, centerY - (height * 0.3f * animationScale) / 2),
                    size = Size(pulseWidth, height * 0.3f * animationScale),
                    cornerRadius = CornerRadius(cornerRadius)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(width * 0.1f + pulseWidth + gap, centerY - (height * 0.3f * animationScale) / 2),
                    size = Size(pulseWidth, height * 0.3f * animationScale),
                    cornerRadius = CornerRadius(cornerRadius)
                )
            }
            "HEARTBEAT" -> {
                // Short thump, then longer pulse
                val thumpWidth = width * 0.15f
                val pulseWidth = width * 0.5f
                val gap = width * 0.1f
                drawRoundRect(
                    color = color.copy(alpha = 0.7f),
                    topLeft = Offset(width * 0.1f, centerY - (height * 0.2f * animationScale) / 2),
                    size = Size(thumpWidth, height * 0.2f * animationScale),
                    cornerRadius = CornerRadius(cornerRadius)
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(width * 0.1f + thumpWidth + gap, centerY - (height * 0.4f * animationScale) / 2),
                    size = Size(pulseWidth, height * 0.4f * animationScale),
                    cornerRadius = CornerRadius(cornerRadius)
                )
            }
            "STACCATO" -> {
                // Three quick sharp taps
                val tapWidth = width * 0.2f
                val gap = width * 0.05f
                repeat(3) { i ->
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(width * 0.1f + i * (tapWidth + gap), centerY - (height * 0.5f * animationScale) / 2),
                        size = Size(tapWidth, height * 0.5f * animationScale),
                        cornerRadius = CornerRadius(cornerRadius)
                    )
                }
            }
            else -> { // BASIC
                // Single solid block
                val pulseWidth = width * 0.8f
                drawRoundRect(
                    color = color,
                    topLeft = Offset(width * 0.1f, centerY - (height * 0.25f * animationScale) / 2),
                    size = Size(pulseWidth, height * 0.25f * animationScale),
                    cornerRadius = CornerRadius(cornerRadius)
                )
            }
        }
    }
}

@Composable
fun VibrationPatternGallery(
    selectedPattern: String,
    onPatternSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(android.os.Vibrator::class.java)!! }
    val scope = rememberCoroutineScope()
    var previewingPattern by remember { mutableStateOf<String?>(null) }

    val patterns = listOf(
        VibrationPatternData("BASIC", stringResource(R.string.vibration_pattern_basic), "📳"),
        VibrationPatternData("RAPID_PULSE", stringResource(R.string.vibration_pattern_pulse), "〰️"),
        VibrationPatternData("HEARTBEAT", stringResource(R.string.vibration_pattern_heartbeat), "💓"),
        VibrationPatternData("STACCATO", stringResource(R.string.vibration_pattern_staccato), "⚡")
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(patterns) { patternData ->
                VibrationPatternCard(
                    data = patternData,
                    isSelected = selectedPattern == patternData.id,
                    isAnimating = previewingPattern == patternData.id,
                    onClick = {
                        onPatternSelected(patternData.id)
                        scope.launch {
                            previewingPattern = patternData.id
                            performHapticPreview(vibrator, patternData.id)
                            previewingPattern = null
                        }
                    }
                )
            }
        }
    }
}

data class VibrationPatternData(val id: String, val label: String, val icon: String)

@Composable
fun VibrationPatternCard(
    data: VibrationPatternData,
    isSelected: Boolean,
    isAnimating: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "bg"
    )
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "border"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(2.dp, borderColor) else null,
        modifier = Modifier
            .width(130.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(data.icon, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    data.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            VibrationWaveform(
                pattern = data.id,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                isAnimated = isAnimating
            )
        }
    }
}

private suspend fun performHapticPreview(vibrator: Vibrator, patternId: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        when (patternId) {
            "RAPID_PULSE" -> {
                vibrator.vibrate(VibrationEffect.createOneShot(80, 180))
                delay(100)
                vibrator.vibrate(VibrationEffect.createOneShot(80, 220))
            }
            "HEARTBEAT" -> {
                vibrator.vibrate(VibrationEffect.createOneShot(60, 150))
                delay(120)
                vibrator.vibrate(VibrationEffect.createOneShot(120, 255))
            }
            "STACCATO" -> {
                repeat(3) {
                    vibrator.vibrate(VibrationEffect.createOneShot(40, 200))
                    delay(80)
                }
            }
            else -> { // BASIC
                vibrator.vibrate(VibrationEffect.createOneShot(200, 180))
            }
        }
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
    }
}
