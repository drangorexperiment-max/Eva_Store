package com.evastore.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Shop
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import java.util.Locale

val Market.icon: ImageVector
    get() = when (this) {
        Market.FDROID -> Icons.Rounded.Android
        Market.GITHUB -> Icons.Rounded.Code
        Market.RUSTORE -> Icons.Rounded.Storefront
        Market.APTOIDE -> Icons.Rounded.Widgets
        Market.APKPURE -> Icons.Rounded.Public
        Market.GETAPPS -> Icons.Rounded.Shop
        Market.GOOGLE_PLAY -> Icons.Rounded.PlayArrow
    }

/** Читаемый размер файла: 12 МБ, 1,4 ГБ. */
fun formatBytes(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1 -> String.format(Locale("ru"), "%.1f ГБ", gb)
        mb >= 1 -> String.format(Locale("ru"), "%.0f МБ", mb)
        else -> String.format(Locale("ru"), "%.0f КБ", kb)
    }
}

/** Читаемое число загрузок: 5 тыс., 10 млн, 1 млрд. */
fun formatDownloads(count: Long): String = when {
    count >= 1_000_000_000 -> "${count / 1_000_000_000} млрд+"
    count >= 1_000_000 -> "${count / 1_000_000} млн+"
    count >= 1_000 -> "${count / 1_000} тыс.+"
    else -> count.toString()
}

@Composable
fun MarketBadge(market: Market, modifier: Modifier = Modifier) {
    val color = Color(market.brandColor)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = market.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = market.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AppIcon(iconUrl: String?, name: String, size: Int = 56) {
    if (iconUrl != null) {
        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape((size * 0.22f).dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(RoundedCornerShape((size * 0.22f).dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Плоский элемент списка в стиле Google Play:
 * иконка слева, название, вторая строка — маркет и размер.
 */
@Composable
fun AppListItem(
    app: StoreApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(app.iconUrl, app.name)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (app.summary.isNotBlank()) {
                    Text(
                        text = app.summary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val marketsText =
                    if (app.options.size > 2)
                        app.options.take(2).joinToString(" · ") { it.market.label } +
                            " +${app.options.size - 2}"
                    else
                        app.options.joinToString(" · ") { it.market.label }
                val meta = listOfNotNull(marketsText, formatBytes(app.sizeBytes))
                    .joinToString("  •  ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
