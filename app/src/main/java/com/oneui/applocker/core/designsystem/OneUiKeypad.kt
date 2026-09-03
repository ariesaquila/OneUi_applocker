package com.oneui.applocker.core.designsystem

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.applocker.R
import com.oneui.applocker.core.theme.OneUiBlue
import com.oneui.applocker.core.theme.OneUiKeypadButtonDark
import com.oneui.applocker.core.theme.OneUiKeypadButtonLight
import com.oneui.applocker.core.theme.OneUiRed

/**
 * PIN dots indicator with bounce and color feedback.
 */
@Composable
fun OneUiPinIndicator(
    length: Int,
    currentLength: Int,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until length) {
            val isFilled = i < currentLength
            val dotSize by animateDpAsState(
                targetValue = if (isFilled) 16.dp else 12.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
                label = "dotSize"
            )

            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> OneUiRed
                    isFilled -> OneUiBlue
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                },
                label = "dotColor"
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * Circular keypad button tailored to One UI lock aesthetic.
 */
@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subText: String? = null
) {
    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val buttonColor = if (isDark) OneUiKeypadButtonDark else OneUiKeypadButtonLight

    Box(
        modifier = modifier
            .size(75.dp)
            .clip(CircleShape)
            .background(buttonColor)
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subText != null) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Samsung One UI PIN Keypad (1 to 9, Biometrics, 0, Backspace).
 */
@Composable
fun OneUiPinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBiometricButton: Boolean = false,
    onBiometricClick: () -> Unit = {}
) {
    val view = LocalView.current

    val keypadRows = listOf(
        listOf("1" to "", "2" to "ABC", "3" to "DEF"),
        listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
        listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ")
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        keypadRows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                row.forEach { (digit, sub) ->
                    KeypadButton(
                        text = digit,
                        subText = sub.ifEmpty { null },
                        onClick = { onDigitClick(digit) }
                    )
                }
            }
        }

        // Bottom row: Biometric / Empty, 0, Backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBiometricButton) {
                Box(
                    modifier = Modifier
                        .size(75.dp)
                        .clip(CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onBiometricClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_fingerprint),
                        contentDescription = "Biyometrik Giriş",
                        tint = OneUiBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(75.dp))
            }

            KeypadButton(
                text = "0",
                onClick = { onDigitClick("0") }
            )

            Box(
                modifier = Modifier
                    .size(75.dp)
                    .clip(CircleShape)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onDeleteClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_backspace),
                    contentDescription = "Geri Al",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
