package com.oneui.applocker.ui.setup

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneui.applocker.R
import com.oneui.applocker.core.designsystem.OneUiPinIndicator
import com.oneui.applocker.core.designsystem.OneUiPinKeypad
import com.oneui.applocker.core.security.SecurityManager
import com.oneui.applocker.core.theme.OneUiBlue
import com.oneui.applocker.core.theme.OneUiRed

@Composable
fun SetupMasterKeyScreen(
    securityManager: SecurityManager,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(1) } // 1: Enter PIN, 2: Confirm PIN
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currentPin = if (step == 1) firstPin else confirmPin

    fun onDigit(digit: String) {
        if (currentPin.length < 4) {
            val updated = currentPin + digit
            if (step == 1) {
                firstPin = updated
                if (updated.length == 4) {
                    step = 2
                }
            } else {
                confirmPin = updated
                if (updated.length == 4) {
                    if (updated == firstPin) {
                        securityManager.setPin(updated)
                        onSetupComplete()
                    } else {
                        isError = true
                        errorMessage = "PIN'ler eşleşmiyor, lütfen tekrar deneyin"
                        confirmPin = ""
                    }
                }
            }
        }
    }

    fun onDelete() {
        if (step == 1) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                step = 1
                firstPin = ""
            }
        }
        isError = false
        errorMessage = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(OneUiBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_shield),
                    contentDescription = null,
                    tint = OneUiBlue,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (step == 1) "Güvenlik PIN'i Belirleyin" else "PIN'inizi Doğrulayın",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (step == 1) "Kilitli uygulamaları açmak için 4 haneli bir PIN girin" else "Oluşturduğunuz PIN'i tekrar girin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OneUiRed,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OneUiPinIndicator(
                length = 4,
                currentLength = currentPin.length,
                isError = isError
            )

            Spacer(modifier = Modifier.height(40.dp))

            OneUiPinKeypad(
                onDigitClick = ::onDigit,
                onDeleteClick = ::onDelete,
                showBiometricButton = false
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
