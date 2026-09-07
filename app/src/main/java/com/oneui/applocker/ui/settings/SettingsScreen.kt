package com.oneui.applocker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oneui.applocker.R
import com.oneui.applocker.core.designsystem.OneUiCard
import com.oneui.applocker.core.designsystem.OneUiHeader
import com.oneui.applocker.core.designsystem.OneUiSwitch
import com.oneui.applocker.core.security.RelockPolicy
import com.oneui.applocker.core.security.SecurityManager
import com.oneui.applocker.core.theme.OneUiBlue
import com.oneui.applocker.core.theme.OneUiShapes
import com.oneui.applocker.data.model.LockType
import com.oneui.applocker.data.model.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSetupCredentials: (LockType) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settingsState.collectAsState()
    var showSecurityQuestionDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OneUiHeader(
            title = stringResource(R.string.title_settings),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Geri",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Security Group
            item {
                SettingsSectionTitle(title = stringResource(R.string.settings_section_security))
                OneUiCard {
                    // Lock Type (PIN vs PATTERN)
                    Text(
                        text = stringResource(R.string.settings_lock_type),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (viewModel.hasPin()) {
                                    viewModel.onLockTypeSelected(LockType.PIN)
                                } else {
                                    onNavigateToSetupCredentials(LockType.PIN)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (settings.lockType == LockType.PIN) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
                            ),
                            contentDescription = null,
                            tint = if (settings.lockType == LockType.PIN) OneUiBlue else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.settings_lock_type_pin),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (viewModel.hasPattern()) {
                                    viewModel.onLockTypeSelected(LockType.PATTERN)
                                } else {
                                    onNavigateToSetupCredentials(LockType.PATTERN)
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (settings.lockType == LockType.PATTERN) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
                            ),
                            contentDescription = null,
                            tint = if (settings.lockType == LockType.PATTERN) OneUiBlue else MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.settings_lock_type_pattern),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Change Credentials Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSetupCredentials(settings.lockType) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Şifreyi / Deseni Değiştir",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (settings.lockType == LockType.PIN) "Mevcut PIN kodunuzu güncelleyin" else "Mevcut kilit deseninizi güncelleyin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_settings),
                            contentDescription = null,
                            tint = OneUiBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Recovery Security Question Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSecurityQuestionDialog = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_security_question),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (viewModel.hasSecurityQuestion()) {
                                    "Belirlendi: ${viewModel.getSecurityQuestion() ?: ""}"
                                } else {
                                    "Belirlenmedi (Şifre sıfırlama için ekleyin)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (viewModel.hasSecurityQuestion()) OneUiBlue else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield),
                            contentDescription = null,
                            tint = OneUiBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Biometrics Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_biometrics),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_biometrics_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OneUiSwitch(
                            checked = settings.isBiometricEnabled,
                            onCheckedChange = viewModel::onBiometricToggled
                        )
                    }
                }
            }

            // Lock Behavior Group
            item {
                SettingsSectionTitle(title = stringResource(R.string.settings_section_behavior))
                OneUiCard {
                    Text(
                        text = stringResource(R.string.settings_relock_policy),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val policies = listOf(
                        RelockPolicy.IMMEDIATELY to stringResource(R.string.relock_immediately),
                        RelockPolicy.SCREEN_OFF to stringResource(R.string.relock_screen_off),
                        RelockPolicy.AFTER_ONE_MINUTE to stringResource(R.string.relock_after_one_min),
                        RelockPolicy.AFTER_FIVE_MINUTES to stringResource(R.string.relock_after_five_min)
                    )

                    policies.forEach { (policy, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onRelockPolicySelected(policy) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (settings.relockPolicy == policy) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
                                ),
                                contentDescription = null,
                                tint = if (settings.relockPolicy == policy) OneUiBlue else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )

                    // Vibration Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_vibration),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.settings_vibration_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OneUiSwitch(
                            checked = settings.isVibrationEnabled,
                            onCheckedChange = viewModel::onVibrationToggled
                        )
                    }
                }
            }

            // Theme / Appearance Group
            item {
                SettingsSectionTitle(title = stringResource(R.string.settings_section_appearance))
                OneUiCard {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val themes = listOf(
                        ThemeMode.SYSTEM to (stringResource(R.string.theme_system) to stringResource(R.string.theme_system_desc)),
                        ThemeMode.LIGHT to (stringResource(R.string.theme_light) to null),
                        ThemeMode.DARK to (stringResource(R.string.theme_dark) to null),
                        ThemeMode.AMOLED to (stringResource(R.string.theme_amoled) to stringResource(R.string.theme_amoled_desc))
                    )

                    themes.forEach { (mode, pair) ->
                        val (label, desc) = pair
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onThemeModeSelected(mode) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (settings.themeMode == mode) R.drawable.ic_radio_checked else R.drawable.ic_radio_unchecked
                                ),
                                contentDescription = null,
                                tint = if (settings.themeMode == mode) OneUiBlue else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (desc != null) {
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Permissions Status Row
            item {
                SettingsSectionTitle(title = "SİSTEM İZİNLERİ")
                OneUiCard(
                    onClick = onNavigateToPermissions
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "İzin Durumunu Görüntüle",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Gerekli erişilebilirlik, bildirim ve kullanım izinlerini kontrol edin",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            painter = painterResource(id = R.drawable.ic_shield),
                            contentDescription = null,
                            tint = OneUiBlue,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        if (showSecurityQuestionDialog) {
            SecurityQuestionConfigDialog(
                currentQuestion = viewModel.getSecurityQuestion(),
                onSave = { question, answer ->
                    viewModel.saveSecurityQuestion(question, answer)
                    showSecurityQuestionDialog = false
                },
                onDismiss = { showSecurityQuestionDialog = false }
            )
        }
    }
}

@Composable
private fun SecurityQuestionConfigDialog(
    currentQuestion: String?,
    onSave: (question: String, answer: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIndex by remember {
        val initialIdx = SecurityManager.DEFAULT_SECURITY_QUESTIONS.indexOf(currentQuestion)
        mutableStateOf(if (initialIdx >= 0) initialIdx else 0)
    }
    var answer by remember { mutableStateOf("") }
    var isDropdownOpen by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.settings_security_question),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.settings_security_question_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = SecurityManager.DEFAULT_SECURITY_QUESTIONS[selectedIndex],
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.security_question_label)) },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_settings),
                                contentDescription = "Seç",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = OneUiShapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = OneUiBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { isDropdownOpen = true }
                    )

                    DropdownMenu(
                        expanded = isDropdownOpen,
                        onDismissRequest = { isDropdownOpen = false }
                    ) {
                        SecurityManager.DEFAULT_SECURITY_QUESTIONS.forEachIndexed { idx, q ->
                            DropdownMenuItem(
                                text = { Text(q, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedIndex = idx
                                    isDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = answer,
                    onValueChange = {
                        answer = it
                        isError = false
                    },
                    label = { Text("Kurtarma Cevabınız") },
                    placeholder = { Text(stringResource(R.string.security_question_answer_hint)) },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = OneUiShapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OneUiBlue,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (isError) {
                    Text(
                        text = "Lütfen bir cevap girin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (answer.isNotBlank()) {
                        val question = SecurityManager.DEFAULT_SECURITY_QUESTIONS[selectedIndex]
                        onSave(question, answer)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OneUiBlue),
                shape = OneUiShapes.medium
            ) {
                Text(
                    text = stringResource(R.string.btn_save),
                    fontWeight = FontWeight.Bold
                )
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

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        color = OneUiBlue,
        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
    )
}
