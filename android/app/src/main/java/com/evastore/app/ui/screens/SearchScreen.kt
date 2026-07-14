package com.evastore.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.ui.SearchUiState
import com.evastore.app.ui.components.AppListItem
import com.evastore.app.ui.components.icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onToggleMarket: (Market) -> Unit,
    onIconPicked: (Uri) -> Unit,
    onAppClick: (StoreApp) -> Unit,
    contentPadding: PaddingValues
) {
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onIconPicked) }

    val listState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Поиск приложений и игр...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                Row {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Очистить")
                        }
                    }
                    IconButton(
                        onClick = {
                            imagePicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    ) {
                        Icon(
                            Icons.Rounded.ImageSearch,
                            contentDescription = "Поиск по изображению иконки",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        // Фильтр маркетов: все прямые источники, прокручиваемый ряд
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            items(
                listOf(
                    Market.RUSTORE, Market.APKPURE, Market.APTOIDE,
                    Market.FDROID, Market.GITHUB
                )
            ) { market ->
                FilterChip(
                    selected = market in state.selectedMarkets,
                    onClick = { onToggleMarket(market) },
                    label = { Text(market.label) },
                    leadingIcon = {
                        Icon(
                            market.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }

        when {
            state.loading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> CenterMessage(state.error)

            state.iconSearchActive -> {
                if (state.iconMatches.isEmpty()) {
                    CenterMessage("Похожие иконки не найдены")
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Text(
                                text = "Найдено по иконке: ${state.iconMatches.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(state.iconMatches, key = { it.app.id }) { match ->
                            AppListItem(app = match.app, onClick = { onAppClick(match.app) })
                        }
                    }
                }
            }

            state.query.isBlank() -> CenterMessage(
                "Введите название приложения или игры,\nлибо нажмите на иконку камеры для поиска по картинке"
            )

            state.results.isEmpty() -> CenterMessage("Ничего не найдено по запросу «${state.query}»")

            else -> LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.results, key = { it.id }) { app ->
                    AppListItem(app = app, onClick = { onAppClick(app) })
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(24.dp)
        )
    }
}
