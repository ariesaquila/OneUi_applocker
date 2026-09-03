package com.oneui.applocker.ui.lock

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.oneui.applocker.R
import com.oneui.applocker.core.designsystem.OneUiPatternLockView
import com.oneui.applocker.core.designsystem.OneUiPinIndicator
import com.oneui.applocker.core.designsystem.OneUiPinKeypad
import com.oneui.applocker.core.theme.OneUiBlue
import com.oneui.applocker.core.theme.OneUiRed
import com.oneui.applocker.data.model.LockType

@Composable
fun LockScreen(
    viewModel: LockViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // App Icon & Name Preview
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.appIcon != null) {
                    val bitmap = rememberLockAppBitmap(uiState.appIcon)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = uiState.appName,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = null,
                        tint = OneUiBlue,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.appName.ifBlank { "Uygulama Kilitli" },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (uiState.lockType == LockType.PIN) {
                    stringResource(R.string.lock_title_enter_pin)
                } else {
                    stringResource(R.string.lock_title_enter_pattern)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = uiState.errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OneUiRed,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Input Section (Keypad or Pattern)
        if (uiState.lockType == LockType.PIN) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OneUiPinIndicator(
                    length = 4,
                    currentLength = uiState.enteredPin.length,
                    isError = uiState.isError
                )

                Spacer(modifier = Modifier.height(36.dp))

                OneUiPinKeypad(
                    onDigitClick = viewModel::onPinDigit,
                    onDeleteClick = viewModel::onPinDelete,
                    showBiometricButton = uiState.isBiometricEnabled,
                    onBiometricClick = viewModel::requestBiometricPrompt
                )
            }
        } else {
            OneUiPatternLockView(
                onPatternComplete = viewModel::onPatternComplete,
                isError = uiState.isError,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun rememberLockAppBitmap(drawable: Drawable?): androidx.compose.ui.graphics.ImageBitmap? {
    if (drawable == null) return null
    return remember(drawable) {
        try {
            drawable.toBitmap(width = 120, height = 120).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}
