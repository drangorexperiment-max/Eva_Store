package com.evastore.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.evastore.app.data.settings.EvaSettings
import com.evastore.app.ui.theme.ThemeMode

/** Подэкраны настроек. */
private enum class SettingsPage(val title: String) {
    ROOT("Настройки"),
    APPEARANCE("Оформление"),
    NETWORK("Сеть и доступ"),
    DOWNLOADS("Загрузки"),
    SECURITY("Безопасность"),
    ABOUT("О приложении")
}

@Composable
fun SettingsScreen(
    settings: EvaSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onAutoScanChange: (Boolean) -> Unit,
    onAnimationsChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onVtKeyChange: (String) -> Unit,
    onDohChange: (Boolean) -> Unit,
    contentPadding: PaddingValues
) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }

    // Системная кнопка «назад» возвращает в корень настроек.
    BackHandler(enabled = page != SettingsPage.ROOT) { page = SettingsPage.ROOT }

    AnimatedContent(targetState = page, label = "settings-page") { current ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (current != SettingsPage.ROOT) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = { page = SettingsPage.ROOT }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад к настройкам"
                        )
                    }
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            when (current) {
                SettingsPage.ROOT -> RootMenu(onOpen = { page = it })
                SettingsPage.APPEARANCE -> AppearancePage(settings, onThemeChange, onAnimationsChange)
                SettingsPage.NETWORK -> NetworkPage(settings, onDohChange)
                SettingsPage.DOWNLOADS -> DownloadsPage(settings, onWifiOnlyChange)
                SettingsPage.SECURITY -> SecurityPage(settings, onAutoScanChange, onVtKeyChange)
                SettingsPage.ABOUT -> AboutPage()
            }
        }
    }
}

// ---------- Корневое меню ----------

@Composable
private fun RootMenu(onOpen: (SettingsPage) -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            MenuRow(
                icon = Icons.Rounded.Palette,
                title = "Оформление",
                subtitle = "Тема, анимации",
                onClick = { onOpen(SettingsPage.APPEARANCE) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            MenuRow(
                icon = Icons.Rounded.Wifi,
                title = "Сеть и доступ",
                subtitle = "Обход блокировок, DNS",
                onClick = { onOpen(SettingsPage.NETWORK) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            MenuRow(
                icon = Icons.Rounded.Download,
                title = "Загрузки",
                subtitle = "Wi-Fi, хранилище",
                onClick = { onOpen(SettingsPage.DOWNLOADS) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            MenuRow(
                icon = Icons.Rounded.Security,
                title = "Безопасность",
                subtitle = "Антивирус VirusTotal",
                onClick = { onOpen(SettingsPage.SECURITY) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            MenuRow(
                icon = Icons.Rounded.Info,
                title = "О приложении",
                subtitle = "Версия, источники",
                onClick = { onOpen(SettingsPage.ABOUT) }
            )
        }
    }
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
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
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- Подэкраны ----------

@Composable
private fun AppearancePage(
    settings: EvaSettings,
    onThemeChange: (ThemeMode) -> Unit,
    onAnimationsChange: (Boolean) -> Unit
) {
    SettingsCard {
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
    }
    SettingsCard {
        SwitchRow(
            title = "Анимации интерфейса",
            subtitle = "Плавные переходы между экранами",
            checked = settings.animationsEnabled,
            onChange = onAnimationsChange
        )
    }
}

@Composable
private fun NetworkPage(
    settings: EvaSettings,
    onDohChange: (Boolean) -> Unit
) {
    SettingsCard {
        SwitchRow(
            title = "Защищённый DNS (DoH)",
            subtitle = "Шифрует DNS-запросы через Google/Cloudflare и снимает блокировки «по домену». Отключите, если сеть работает нестабильно.",
            checked = settings.dohEnabled,
            onChange = onDohChange
        )
    }
    SettingsCard {
        Text(
            text = "Как это работает",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Защищённый DNS снимает блокировки «по домену» — часть маркетов работает без VPN, хотя и медленнее. Если источник заблокирован по IP, обойти это без VPN невозможно — такие маркеты автоматически пропускаются через несколько секунд, чтобы поиск не зависал. Для ускорения приложение теперь заранее прогревает DNS-кэш при запуске.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DownloadsPage(
    settings: EvaSettings,
    onWifiOnlyChange: (Boolean) -> Unit
) {
    SettingsCard {
        SwitchRow(
            title = "Только по Wi-Fi",
            subtitle = "Не скачивать APK через мобильную сеть",
            checked = settings.wifiOnlyDownloads,
            onChange = onWifiOnlyChange
        )
    }
    SettingsCard {
        Text(
            text = "Проверка файлов",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Каждый скачанный файл проверяется: сервер должен отдать настоящий APK с AndroidManifest.xml внутри. Битые и подменённые файлы отклоняются автоматически.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SecurityPage(
    settings: EvaSettings,
    onAutoScanChange: (Boolean) -> Unit,
    onVtKeyChange: (String) -> Unit
) {
    SettingsCard {
        SwitchRow(
            title = "Автопроверка загрузок",
            subtitle = "Сканировать каждый APK после скачивания через VirusTotal",
            checked = settings.autoScanDownloads,
            onChange = onAutoScanChange
        )
    }
    SettingsCard {
        var keyInput by rememberSaveable(settings.virusTotalApiKey) {
            mutableStateOf(settings.virusTotalApiKey)
        }
        Text(
            text = "API-ключ VirusTotal",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Бесплатный ключ можно получить на virustotal.com в разделе API Key. Хранится только на устройстве. Без ключа отчёт откроется в браузере по хешу файла.",
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
}

@Composable
private fun AboutPage() {
    SettingsCard {
        Text(
            text = "Eva Store 1.2.0",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Агрегатор приложений с прямой загрузкой APK: Google Play (зеркало), RuStore, APKPure, Aptoide, F-Droid и GitHub. Проверка файлов через VirusTotal, обход DNS-блокировок через защищённый DNS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    SettingsCard {
        Text(
            text = "Источники приложений",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Google Play — через зеркало APKPure (те же APK, подписи разработчиков сохранены)\nRuStore — официальный API\nAPKPure, Aptoide — официальные API\nF-Droid — официальный репозиторий\nGitHub — APK из релизов открытых проектов",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- Общие элементы ----------

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
