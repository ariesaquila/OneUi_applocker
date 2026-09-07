package com.oneui.applocker.core.designsystem

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oneui.applocker.core.theme.OneUiBlue
import com.oneui.applocker.core.theme.OneUiRed

/**
 * High performance Samsung One UI 3x3 Pattern Lock View.
 * Supports intermediate dot bridging, auto-clearing on error reset, and smooth 120 FPS gesture drawing.
 */
@Composable
fun OneUiPatternLockView(
    onPatternComplete: (List<Int>) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    isError: Boolean = false,
    isEnabled: Boolean = true,
    dotRadius: Dp = 8.dp,
    hitRadius: Dp = 34.dp
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

    val dotOffsets = remember { Array(9) { Offset.Zero } }

    // Automatically clear drawn pattern when error state resets
    LaunchedEffect(isError) {
        if (!isError) {
            selectedDots.clear()
            currentTouchPosition = null
        }
    }

    fun tryAddDot(hitDot: Int) {
        if (!selectedDots.contains(hitDot)) {
            // Bridge intermediate dot if swiping in a straight line across unvisited midpoint
            if (selectedDots.isNotEmpty()) {
                val last = selectedDots.last()
                val intermediate = getIntermediateDot(last, hitDot)
                if (intermediate != null && !selectedDots.contains(intermediate)) {
                    selectedDots.add(intermediate)
                }
            }
            selectedDots.add(hitDot)
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(isEnabled, isError) {
                if (!isEnabled || isError) return@pointerInput

                detectDragGestures(
                    onDragStart = { startOffset ->
                        selectedDots.clear()
                        currentTouchPosition = startOffset
                        val hitDot = findHitDot(startOffset, size.toPx(), hitRadiusPx)
                        if (hitDot != null) {
                            tryAddDot(hitDot)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        currentTouchPosition = change.position
                        val hitDot = findHitDot(change.position, size.toPx(), hitRadiusPx)
                        if (hitDot != null) {
                            tryAddDot(hitDot)
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
            for (i in 0..8) {
                dotOffsets[i] = Offset(step * ((i % 3) + 1), step * ((i / 3) + 1))
            }

            // Draw connecting lines between selected dots
            if (selectedDots.size > 1) {
                for (i in 0 until selectedDots.size - 1) {
                    val start = dotOffsets[selectedDots[i]]
                    val end = dotOffsets[selectedDots[i + 1]]
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
                val lastDot = dotOffsets[selectedDots.last()]
                drawLine(
                    color = activeColor.copy(alpha = 0.7f),
                    start = lastDot,
                    end = currentTouchPosition!!,
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }

            // Draw dots (3x3 grid)
            for (index in 0..8) {
                val center = dotOffsets[index]
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
    val hitRadiusSq = hitRadius * hitRadius
    for (i in 0..8) {
        val cx = step * ((i % 3) + 1)
        val cy = step * ((i / 3) + 1)
        val dx = offset.x - cx
        val dy = offset.y - cy
        if (dx * dx + dy * dy <= hitRadiusSq) {
            return i
        }
    }
    return null
}

/**
 * Returns the intermediate dot index if dot1 and dot2 have a direct midpoint on a 3x3 grid.
 */
private fun getIntermediateDot(dot1: Int, dot2: Int): Int? {
    val row1 = dot1 / 3
    val col1 = dot1 % 3
    val row2 = dot2 / 3
    val col2 = dot2 % 3

    if ((row1 + row2) % 2 == 0 && (col1 + col2) % 2 == 0) {
        val midRow = (row1 + row2) / 2
        val midCol = (col1 + col2) / 2
        val midDot = midRow * 3 + midCol
        if (midDot != dot1 && midDot != dot2) {
            return midDot
        }
    }
    return null
}
