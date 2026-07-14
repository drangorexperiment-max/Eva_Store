package com.evastore.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evastore.app.data.model.StoreApp
import com.evastore.app.ui.HomeUiState
import com.evastore.app.ui.components.AppIcon
import com.evastore.app.ui.components.AppListItem
import com.evastore.app.ui.components.formatBytes

@Composable
fun HomeScreen(
    state: HomeUiState,
    onAppClick: (StoreApp) -> Unit,
    onRetry: () -> Unit,
    contentPadding: PaddingValues
) {
    when {
        state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRetry) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Повторить", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        else -> LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "featured-title") {
                Text(
                    text = "Рекомендуем для вас",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            item(key = "featured-row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(state.featured.take(12), key = { "f-${it.id}" }) { app ->
                        FeaturedCard(app = app, onClick = { onAppClick(app) })
                    }
                }
            }

            item(key = "all-title") {
                Text(
                    text = "Популярные приложения",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(state.featured.drop(12), key = { it.id }) { app ->
                AppListItem(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}

@Composable
private fun FeaturedCard(app: StoreApp, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(96.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AppIcon(app.iconUrl, app.name, size = 96)
            Text(
                text = app.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            formatBytes(app.sizeBytes)?.let { size ->
                Text(
                    text = size,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
