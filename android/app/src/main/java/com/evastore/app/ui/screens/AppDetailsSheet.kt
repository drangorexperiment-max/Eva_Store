package com.evastore.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.SourceType
import com.evastore.app.data.model.StoreApp
import com.evastore.app.ui.components.AppIcon
import com.evastore.app.ui.components.formatBytes
import com.evastore.app.ui.components.formatDownloads
import com.evastore.app.ui.components.icon

/**
 * Карточка приложения в стиле Google Play: шапка с иконкой и статистикой,
 * большая кнопка «Установить» (раскрывает выбор маркета с весом файла
 * от каждого источника), скриншоты и описание.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailsSheet(
    app: StoreApp,
    onDownload: (StoreApp, DownloadOption) -> Unit,
    onDismiss: () -> Unit
) {
    var showSources by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Шапка: иконка + название + разработчик
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(app.iconUrl, app.name, size = 72)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
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

            // Полоса статистики: рейтинг | загрузки | размер (как в Google Play)
            StatsRow(app)

            // Кнопка «Установить» — раскрывает выбор источника
            Button(
                onClick = { showSources = !showSources },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (showSources) "Выберите источник ниже" else "Установить",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            AnimatedVisibility(visible = showSources) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    app.options.forEach { option ->
                        MarketDownloadRow(
                            option = option,
                            onClick = { onDownload(app, option) }
                        )
                    }
                }
            }

            // Скриншоты (обложки), как в Google Play
            if (app.screenshots.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(app.screenshots) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Скриншот приложения ${app.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .height(220.dp)
                                .width(124.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            if (app.summary.isNotBlank()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Рейтинг | загрузки | размер — разделённые вертикальными линиями. */
@Composable
private fun StatsRow(app: StoreApp) {
    val size = app.options.firstNotNullOfOrNull { it.sizeBytes }
    val hasAny = app.rating != null || app.downloads != null || size != null
    if (!hasAny) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.rating?.let { rating ->
            StatCell(
                top = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "%.1f".format(rating),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                label = "Рейтинг"
            )
            StatDivider()
        }
        app.downloads?.let { downloads ->
            StatCell(
                top = {
                    Text(
                        text = formatDownloads(downloads),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                label = "Загрузок"
            )
            StatDivider()
        }
        size?.let { bytes ->
            StatCell(
                top = {
                    Text(
                        text = formatBytes(bytes).orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                label = "Размер"
            )
        }
    }
}

@Composable
private fun StatCell(top: @Composable () -> Unit, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        top()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatDivider() {
    Box(modifier = Modifier.height(24.dp).width(1.dp)) {
        HorizontalDivider(
            modifier = Modifier
                .height(24.dp)
                .width(1.dp)
        )
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
