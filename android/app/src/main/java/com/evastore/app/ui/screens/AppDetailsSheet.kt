package com.evastore.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.SourceType
import com.evastore.app.data.model.StoreApp
import com.evastore.app.ui.components.AppIcon
import com.evastore.app.ui.components.formatBytes
import com.evastore.app.ui.components.icon

/**
 * Карточка приложения: описание и выбор маркета для скачивания.
 * DIRECT_APK — скачивается внутри Eva Store, STOREFRONT — переход в маркет.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsSheet(
    app: StoreApp,
    onDownload: (StoreApp, DownloadOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(app.iconUrl, app.name, size = 72)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    app.developer?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    app.packageName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (app.summary.isNotBlank()) {
                Text(
                    text = app.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Выберите источник загрузки",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                app.options.forEach { option ->
                    MarketDownloadRow(
                        option = option,
                        onClick = { onDownload(app, option) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketDownloadRow(option: DownloadOption, onClick: () -> Unit) {
    val color = Color(option.market.brandColor)
    val direct = option.market.type == SourceType.DIRECT_APK

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.market.icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.market.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subtitle = buildString {
                    append(
                        if (direct) "Скачать APK через Eva Store"
                        else "Открыть страницу в маркете"
                    )
                    formatBytes(option.sizeBytes)?.let { append("  •  $it") }
                    option.versionName?.let { append("  •  v$it") }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (direct) Icons.Rounded.Download else Icons.Rounded.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
