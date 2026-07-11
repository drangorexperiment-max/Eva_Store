package com.evastore.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.evastore.app.data.settings.EvaSettings
import com.evastore.app.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    settings: EvaSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onAutoScanChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onVtKeyChange: (String) -> Unit,
    contentPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSection("Оформление") {
            Text(
                text = "Тема приложения",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    ThemeMode.SYSTEM to "Системная",
                    ThemeMode.LIGHT to "Светлая",
                    ThemeMode.DARK to "Тёмная"
                )
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = settings.themeMode == mode,
                        onClick = { onThemeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size)
                    ) { Text(label) }
                }
            }
            SwitchRow(
                title = "Анимации интерфейса",
                subtitle = "Плавные переходы между экранами",
                checked = settings.animationsEnabled,
                onChange = onAnimationsChange
            )
        }

        SettingsSection("Загрузки") {
            SwitchRow(
                title = "Только по Wi-Fi",
                subtitle = "Не скачивать APK через мобильную сеть",
                checked = settings.wifiOnlyDownloads,
                onChange = onWifiOnlyChange
            )
        }

        SettingsSection("Антивирус (VirusTotal)") {
            SwitchRow(
                title = "Автопроверка загрузок",
                subtitle = "Сканировать каждый APK после скачивания",
                checked = settings.autoScanDownloads,
                onChange = onAutoScanChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            var keyInput by rememberSaveable(settings.virusTotalApiKey) {
                mutableStateOf(settings.virusTotalApiKey)
            }
            Text(
                text = "API-ключ VirusTotal",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Бесплатный ключ можно получить на virustotal.com в разделе API Key. Хранится только на устройстве.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                placeholder = { Text("Вставьте API-ключ...") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = { onVtKeyChange(keyInput) },
                enabled = keyInput != settings.virusTotalApiKey
            ) { Text("Сохранить ключ") }
        }

        SettingsSection("О приложении") {
            Text(
                text = "Eva Store 1.0.0",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Агрегатор приложений: F-Droid, RuStore и GitHub с прямой загрузкой APK; Google Play, GetApps и App Store — как витрины. Проверка файлов через VirusTotal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
