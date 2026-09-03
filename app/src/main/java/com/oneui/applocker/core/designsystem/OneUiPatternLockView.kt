package com.oneui.applocker.core.designsystem

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oneui.applocker.core.theme.OneUiBlue
import com.oneui.applocker.core.theme.OneUiRed
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * High performance Samsung One UI 3x3 Pattern Lock View.
 */
@Composable
fun OneUiPatternLockView(
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    isError: Boolean = false,
    dotRadius: Dp = 8.dp,
    hitRadius: Dp = 32.dp
) {
    val view = LocalView.current
    val density = LocalDensity.current
    val hitRadiusPx = with(density) { hitRadius.toPx() }
    val dotRadiusPx = with(density) { dotRadius.toPx() }
    val strokeWidthPx = with(density) { 4.dp.toPx() }

    val selectedDots = remember { mutableStateListOf<Int>() }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }

    val defaultDotColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val activeColor = if (isError) OneUiRed else OneUiBlue

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        selectedDots.clear()
                        currentTouchPosition = startOffset
                        val hitDot = findHitDot(startOffset, size.toPx(), hitRadiusPx)
                        if (hitDot != null && !selectedDots.contains(hitDot)) {
                            selectedDots.add(hitDot)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        currentTouchPosition = pos
                        val hitDot = findHitDot(pos, size.toPx(), hitRadiusPx)
                        if (hitDot != null && !selectedDots.contains(hitDot)) {
                            selectedDots.add(hitDot)
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                    },
                    onDragEnd = {
                        if (selectedDots.isNotEmpty()) {
                            onPatternComplete(selectedDots.toList())
                        }
                        currentTouchPosition = null
                    },
                    onDragCancel = {
                        selectedDots.clear()
                        currentTouchPosition = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.width
            val step = canvasSize / 4f
            val dotPositions = (0..8).map { index ->
                val row = index / 3
                val col = index % 3
                Offset(step * (col + 1), step * (row + 1))
            }

            // Draw connecting lines between selected dots
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val start = dotPositions[selectedDots[i]]
                    val end = dotPositions[selectedDots[i + 1]]
                    drawLine(
                        color = activeColor,
                        start = start,
                        end = end,
                        strokeWidth = strokeWidthPx,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw line to current touch finger position
            if (selectedDots.isNotEmpty() && currentTouchPosition != null) {
                val lastDot = dotPositions[selectedDots.last()]
                drawLine(
                    color = activeColor.copy(alpha = 0.7f),
                    start = lastDot,
                    end = currentTouchPosition!!,
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // Draw dots (3x3 grid)
            dotPositions.forEachIndexed { index, center ->
                val isSelected = selectedDots.contains(index)
                val dotColor = if (isSelected) activeColor else defaultDotColor

                // Outer circle for selected dot
                if (isSelected) {
                    drawCircle(
                        color = dotColor.copy(alpha = 0.2f),
                        radius = dotRadiusPx * 2.5f,
                        center = center
                    )
                }

                drawCircle(
                    color = dotColor,
                    radius = if (isSelected) dotRadiusPx * 1.3f else dotRadiusPx,
                    center = center
                )
            }
        }
    }
}

private fun findHitDot(offset: Offset, canvasSize: Float, hitRadius: Float): Int? {
    val step = canvasSize / 4f
    for (i in 0..8) {
        val row = i / 3
        val col = i % 3
        val center = Offset(step * (col + 1), step * (row + 1))
        val distance = sqrt((offset.x - center.x).pow(2) + (offset.y - center.y).pow(2))
        if (distance <= hitRadius) {
            return i
        }
    }
    return null
}
