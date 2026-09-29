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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    var showForgotDialog by remember { mutableStateOf(false) }

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
                    val bitmap = rememberLockAppBitmap(uiState.targetPackage, uiState.appIcon)
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
                text = uiState.appName.ifBlank { stringResource(R.string.app_locked_placeholder) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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

                Spacer(modifier = Modifier.height(28.dp))

                OneUiPinKeypad(
                    onDigitClick = viewModel::onPinDigit,
                    onDeleteClick = viewModel::onPinDelete,
                    showBiometricButton = uiState.isBiometricEnabled,
                    onBiometricClick = viewModel::requestBiometricPrompt
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = { showForgotDialog = true }) {
                    Text(
                        text = stringResource(R.string.forgot_password),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = OneUiBlue
                    )
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                OneUiPatternLockView(
                    onPatternComplete = viewModel::onPatternComplete,
                    isError = uiState.isError,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isBiometricEnabled) {
                        IconButton(
                            onClick = viewModel::requestBiometricPrompt,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_fingerprint),
                                contentDescription = stringResource(R.string.biometric_prompt_title),
                                tint = OneUiBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    TextButton(onClick = { showForgotDialog = true }) {
                        Text(
                            text = stringResource(R.string.forgot_password),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUiBlue
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (showForgotDialog) {
            ForgotPasswordDialog(
                viewModel = viewModel,
                isBiometricEnabled = uiState.isBiometricEnabled,
                onDismiss = { showForgotDialog = false }
            )
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    viewModel: LockViewModel,
    isBiometricEnabled: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasQuestion = viewModel.hasSecurityQuestion()
    val question = viewModel.getSecurityQuestion()
    var answer by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OneUiBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_shield),
                        contentDescription = null,
                        tint = OneUiBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.reset_credentials_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (hasQuestion && question != null) {
                    Text(
                        text = stringResource(R.string.reset_credentials_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(14.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.security_question_label),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = OneUiBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = answer,
                        onValueChange = {
                            answer = it
                            isError = false
                        },
                        label = { Text(stringResource(R.string.security_question_answer_label)) },
                        placeholder = { Text(stringResource(R.string.security_question_answer_hint)) },
                        singleLine = true,
                        isError = isError,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OneUiBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    if (isError) {
                        Text(
                            text = stringResource(R.string.security_question_wrong),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            if (viewModel.verifySecurityAnswer(answer)) {
                                onDismiss()
                                viewModel.resetCredentialsAndOpenSetup(context)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.btn_verify_and_reset),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = OneUiBlue
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.security_question_not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isBiometricEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onDismiss()
                                viewModel.requestBiometricPrompt()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_fingerprint),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_verify_with_biometrics))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hasQuestion) {
                Button(
                    onClick = {
                        if (viewModel.verifySecurityAnswer(answer)) {
                            onDismiss()
                            viewModel.onRecoveryUnlock()
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_unlock),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

private val lockBitmapCache = android.util.LruCache<String, androidx.compose.ui.graphics.ImageBitmap>(30)

@Composable
private fun rememberLockAppBitmap(packageName: String, drawable: Drawable?): androidx.compose.ui.graphics.ImageBitmap? {
    if (drawable == null) return null
    return remember(packageName) {
        val cached = lockBitmapCache.get(packageName)
        if (cached != null) {
            cached
        } else {
            try {
                val bitmap = drawable.toBitmap(width = 120, height = 120).asImageBitmap()
                lockBitmapCache.put(packageName, bitmap)
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }
}
