package com.oneui.applocker.ui.setup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oneui.applocker.AppLockerApp
import com.oneui.applocker.R
import com.oneui.applocker.core.designsystem.OneUiPatternLockView
import com.oneui.applocker.core.designsystem.OneUiPinIndicator
import com.oneui.applocker.core.designsystem.OneUiPinKeypad
import com.oneui.applocker.core.security.SecurityManager
import com.oneui.applocker.core.theme.OneUiBlue
import com.oneui.applocker.core.theme.OneUiRed
import com.oneui.applocker.core.theme.OneUiShapes
import com.oneui.applocker.data.model.LockType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class SetupMode {
    PIN,
    PATTERN
}

private enum class SetupPhase {
    ENTER,
    CONFIRM,
    RECOVERY_QUESTION
}

@Composable
fun SetupMasterKeyScreen(
    securityManager: SecurityManager,
    onSetupComplete: () -> Unit,
    modifier: Modifier = Modifier,
    initialLockType: LockType = LockType.PIN,
    isChangeMode: Boolean = false,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsRepo = (context.applicationContext as AppLockerApp).settingsRepository

    var selectedMode by remember {
        mutableStateOf(if (initialLockType == LockType.PATTERN) SetupMode.PATTERN else SetupMode.PIN)
    }
    var phase by remember { mutableStateOf(SetupPhase.ENTER) }

    // PIN State
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    // Pattern State
    var firstPattern by remember { mutableStateOf<List<Int>>(emptyList()) }
    var confirmPattern by remember { mutableStateOf<List<Int>>(emptyList()) }

    // Recovery Question State
    var selectedQuestionIndex by remember { mutableStateOf(0) }
    var customQuestion by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var isQuestionDropdownOpen by remember { mutableStateOf(false) }

    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerError(message: String) {
        isError = true
        errorMessage = message
        coroutineScope.launch {
            delay(900)
            isError = false
            errorMessage = null
        }
    }

    // PIN Digit Input
    fun onPinDigit(digit: String) {
        if (isError) return
        val current = if (phase == SetupPhase.ENTER) firstPin else confirmPin
        if (current.length < 4) {
            val updated = current + digit
            if (phase == SetupPhase.ENTER) {
                firstPin = updated
                if (updated.length == 4) {
                    phase = SetupPhase.CONFIRM
                }
            } else {
                confirmPin = updated
                if (updated.length == 4) {
                    if (updated == firstPin) {
                        securityManager.setPin(updated)
                        coroutineScope.launch {
                            settingsRepo.setLockType(LockType.PIN)
                        }
                        if (securityManager.hasSecurityQuestion()) {
                            onSetupComplete()
                        } else {
                            phase = SetupPhase.RECOVERY_QUESTION
                        }
                    } else {
                        triggerError("PIN\'ler eşleşmiyor, lütfen tekrar deneyin")
                        confirmPin = ""
                    }
                }
            }
        }
    }

    fun onPinDelete() {
        if (phase == SetupPhase.ENTER) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            } else {
                phase = SetupPhase.ENTER
                firstPin = ""
            }
        }
        isError = false
        errorMessage = null
    }

    // Pattern Input
    fun onPatternDrawn(pattern: List<Int>) {
        if (isError) return

        if (pattern.size < 4) {
            triggerError("En az 4 nokta birleştirmelisiniz")
            return
        }

        if (phase == SetupPhase.ENTER) {
            firstPattern = pattern
            phase = SetupPhase.CONFIRM
        } else if (phase == SetupPhase.CONFIRM) {
            confirmPattern = pattern
            if (pattern == firstPattern) {
                securityManager.setPattern(pattern)
                coroutineScope.launch {
                    settingsRepo.setLockType(LockType.PATTERN)
                }
                if (securityManager.hasSecurityQuestion()) {
                    onSetupComplete()
                } else {
                    phase = SetupPhase.RECOVERY_QUESTION
                }
            } else {
                triggerError("Desenler eşleşmedi, lütfen baştan deneyin")
                coroutineScope.launch {
                    delay(800)
                    phase = SetupPhase.ENTER
                    firstPattern = emptyList()
                    confirmPattern = emptyList()
                }
            }
        }
    }

    fun saveRecoveryQuestion() {
        val question = if (selectedQuestionIndex < SecurityManager.DEFAULT_SECURITY_QUESTIONS.size) {
            SecurityManager.DEFAULT_SECURITY_QUESTIONS[selectedQuestionIndex]
        } else {
            customQuestion.ifBlank { "Güvenlik Sorusu" }
        }
        if (recoveryAnswer.isNotBlank()) {
            securityManager.setSecurityQuestion(question, recoveryAnswer)
        }
        onSetupComplete()
    }

    val currentPin = if (phase == SetupPhase.ENTER) firstPin else confirmPin

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (phase == SetupPhase.RECOVERY_QUESTION) Arrangement.Top else Arrangement.SpaceBetween
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Geri",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(36.dp))
        }

        // Header & Mode Selector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lock Type Selector (PIN or Pattern) - only available in ENTER phase
            if (phase != SetupPhase.RECOVERY_QUESTION) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val pinSelected = selectedMode == SetupMode.PIN
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (pinSelected) OneUiBlue else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selectedMode = SetupMode.PIN
                                phase = SetupPhase.ENTER
                                firstPin = ""
                                confirmPin = ""
                                isError = false
                                errorMessage = null
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "PIN Kodu",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (pinSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (pinSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val patternSelected = selectedMode == SetupMode.PATTERN
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (patternSelected) OneUiBlue else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                selectedMode = SetupMode.PATTERN
                                phase = SetupPhase.ENTER
                                firstPattern = emptyList()
                                confirmPattern = emptyList()
                                isError = false
                                errorMessage = null
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Desen",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (patternSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (patternSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Title & Instruction
            val titleText = when (phase) {
                SetupPhase.ENTER -> if (selectedMode == SetupMode.PIN) "Güvenlik PIN\'i Belirleyin" else "Kilit Desenini Çizin"
                SetupPhase.CONFIRM -> if (selectedMode == SetupMode.PIN) "PIN\'inizi Doğrulayın" else "Deseni Doğrulayın"
                SetupPhase.RECOVERY_QUESTION -> "Kurtarma Güvenlik Sorusu"
            }

            val subtitleText = when (phase) {
                SetupPhase.ENTER -> if (selectedMode == SetupMode.PIN) "Kilitli uygulamaları açmak için 4 haneli bir PIN girin" else "En az 4 noktayı birleştirerek bir desen oluşturun"
                SetupPhase.CONFIRM -> if (selectedMode == SetupMode.PIN) "Oluşturduğunuz PIN\'i onaylamak için tekrar girin" else "Oluşturduğunuz deseni onaylamak için tekrar çizin"
                SetupPhase.RECOVERY_QUESTION -> "Şifrenizi unutursanız sıfırlayabilmek için bir kurtarma sorusu belirleyin"
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = OneUiRed,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }

        // Body Content based on Phase & Mode
        when (phase) {
            SetupPhase.RECOVERY_QUESTION -> {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Question Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (selectedQuestionIndex < SecurityManager.DEFAULT_SECURITY_QUESTIONS.size) {
                                SecurityManager.DEFAULT_SECURITY_QUESTIONS[selectedQuestionIndex]
                            } else {
                                customQuestion
                            },
                            onValueChange = { customQuestion = it },
                            label = { Text("Güvenlik Sorusu") },
                            readOnly = selectedQuestionIndex < SecurityManager.DEFAULT_SECURITY_QUESTIONS.size,
                            trailingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_settings),
                                    contentDescription = "Soruları Gör",
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { isQuestionDropdownOpen = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isQuestionDropdownOpen = true },
                            shape = OneUiShapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUiBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        if (selectedQuestionIndex < SecurityManager.DEFAULT_SECURITY_QUESTIONS.size) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isQuestionDropdownOpen = true }
                            )
                        }

                        DropdownMenu(
                            expanded = isQuestionDropdownOpen,
                            onDismissRequest = { isQuestionDropdownOpen = false }
                        ) {
                            SecurityManager.DEFAULT_SECURITY_QUESTIONS.forEachIndexed { index, q ->
                                DropdownMenuItem(
                                    text = { Text(q, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedQuestionIndex = index
                                        isQuestionDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Answer Field
                    OutlinedTextField(
                        value = recoveryAnswer,
                        onValueChange = { recoveryAnswer = it },
                        label = { Text("Cevabınız") },
                        placeholder = { Text("Kurtarma cevabını girin") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { saveRecoveryQuestion() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = OneUiShapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OneUiBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = ::saveRecoveryQuestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = OneUiShapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue)
                    ) {
                        Text(
                            text = if (recoveryAnswer.isNotBlank()) "Kaydet ve Tamamla" else "Tamamla",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(onClick = onSetupComplete) {
                        Text(
                            text = "Şimdilik Atla",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SetupPhase.ENTER, SetupPhase.CONFIRM -> {
                if (selectedMode == SetupMode.PIN) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        OneUiPinIndicator(
                            length = 4,
                            currentLength = currentPin.length,
                            isError = isError
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        OneUiPinKeypad(
                            onDigitClick = ::onPinDigit,
                            onDeleteClick = ::onPinDelete,
                            showBiometricButton = false
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        OneUiPatternLockView(
                            onPatternComplete = ::onPatternDrawn,
                            isError = isError,
                            size = 280.dp
                        )

                        if (phase == SetupPhase.CONFIRM) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    phase = SetupPhase.ENTER
                                    firstPattern = emptyList()
                                    confirmPattern = emptyList()
                                    isError = false
                                    errorMessage = null
                                }
                            ) {
                                Text(
                                    text = "Yeniden Çiz",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = OneUiBlue
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
