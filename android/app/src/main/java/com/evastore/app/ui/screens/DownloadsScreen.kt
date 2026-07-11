package com.evastore.app.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evastore.app.data.download.DownloadStatus
import com.evastore.app.data.download.DownloadTask
import com.evastore.app.data.model.ScanState
import com.evastore.app.ui.components.AppIcon

@Composable
fun DownloadsScreen(
    tasks: List<DownloadTask>,
    scanStates: Map<String, ScanState>,
    onInstall: (DownloadTask) -> Unit,
    onScan: (DownloadTask) -> Unit,
    onRemove: (DownloadTask) -> Unit,
    contentPadding: PaddingValues
) {
    if (tasks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.DownloadDone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "Загрузок пока нет",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Скачанные APK появятся здесь",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(tasks.asReversed(), key = { it.id }) { task ->
            DownloadCard(
                task = task,
                scanState = scanStates[task.id] ?: ScanState.Idle,
                onInstall = { onInstall(task) },
                onScan = { onScan(task) },
                onRemove = { onRemove(task) }
            )
        }
    }
}

@Composable
private fun DownloadCard(
    task: DownloadTask,
    scanState: ScanState,
    onInstall: () -> Unit,
    onScan: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(task.iconUrl, task.appName, size = 48)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.appName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${task.marketLabel} - ${statusLabel(task)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Удалить загрузку",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (task.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ScanStatusRow(scanState)

            if (task.status == DownloadStatus.DONE || task.status == DownloadStatus.SCANNING) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onInstall, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Rounded.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("Установить", modifier = Modifier.padding(start = 6.dp))
                    }
                    if (scanState is ScanState.Idle || scanState is ScanState.Error) {
                        OutlinedButton(onClick = onScan, modifier = Modifier.weight(1f)) {
                            Icon(
                                Icons.Rounded.Security,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Проверить", modifier = Modifier.padding(start = 6.dp))
                        }
                    }
                }
            }

            if (task.status == DownloadStatus.FAILED && task.error != null) {
                Text(
                    text = task.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ScanStatusRow(scanState: ScanState) {
    when (scanState) {
        is ScanState.Idle -> Unit

        is ScanState.Uploading, is ScanState.Analyzing -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                text = if (scanState is ScanState.Uploading)
                    "VirusTotal: загрузка файла..." else "VirusTotal: анализ антивирусами...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        is ScanState.Done -> {
            val r = scanState.result
            val clean = r.isClean
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (clean) Icons.Rounded.GppGood else Icons.Rounded.GppMaybe,
                    contentDescription = null,
                    tint = if (clean) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (clean)
                        "Чисто: 0 из ${r.total} антивирусов нашли угрозы"
                    else
                        "Внимание: ${r.malicious} вредоносных, ${r.suspicious} подозрительных из ${r.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (clean) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error
                )
            }
        }

        is ScanState.Error -> Text(
            text = "Сканирование: ${scanState.message}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

private fun statusLabel(task: DownloadTask): String = when (task.status) {
    DownloadStatus.QUEUED -> "В очереди"
    DownloadStatus.DOWNLOADING -> "Скачивание ${(task.progress * 100).toInt()}%"
    DownloadStatus.SCANNING -> "Проверка антивирусом"
    DownloadStatus.DONE -> "Готово к установке"
    DownloadStatus.FAILED -> "Ошибка"
}
