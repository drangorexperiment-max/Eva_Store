package com.evastore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.evastore.app.data.download.DownloadStatus
import com.evastore.app.ui.MainViewModel
import com.evastore.app.ui.screens.AppDetailsSheet
import com.evastore.app.ui.screens.DownloadsScreen
import com.evastore.app.ui.screens.HomeScreen
import com.evastore.app.ui.screens.SearchScreen
import com.evastore.app.ui.screens.SettingsScreen
import com.evastore.app.ui.theme.EvaStoreTheme
import java.io.File

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val activeIcon: ImageVector
)

private val tabs = listOf(
    Tab("home", "Главная", Icons.Outlined.Home, Icons.Rounded.Home),
    Tab("search", "Поиск", Icons.Outlined.Search, Icons.Rounded.Search),
    Tab("downloads", "Загрузки", Icons.Outlined.Download, Icons.Rounded.Download),
    Tab("settings", "Настройки", Icons.Outlined.Settings, Icons.Rounded.Settings)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashFile = File(filesDir, "last_crash.txt")
        val lastCrash = if (crashFile.exists()) crashFile.readText().also { crashFile.delete() } else null

        setContent {
            val viewModel: MainViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            EvaStoreTheme(themeMode = settings.themeMode) {
                var crashText by remember { mutableStateOf(lastCrash) }
                crashText?.let { trace ->
                    AlertDialog(
                        onDismissRequest = { crashText = null },
                        title = { Text("Отчёт о прошлом сбое") },
                        text = {
                            Text(
                                trace.take(2000),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { crashText = null }) { Text("Закрыть") }
                        }
                    )
                }
                EvaStoreApp(viewModel, animationsEnabled = settings.animationsEnabled)
            }
        }
    }
}

@Composable
private fun EvaStoreApp(viewModel: MainViewModel, animationsEnabled: Boolean) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val home by viewModel.home.collectAsStateWithLifecycle()
    val search by viewModel.search.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val scanStates by viewModel.scanStates.collectAsStateWithLifecycle()
    val selectedApp by viewModel.selectedApp.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val activeDownloads = downloads.count { it.status == DownloadStatus.DOWNLOADING }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            if (tab.route == "downloads" && activeDownloads > 0) {
                                BadgedBox(badge = { Badge { Text("$activeDownloads") } }) {
                                    Icon(
                                        if (selected) tab.activeIcon else tab.icon,
                                        contentDescription = tab.label
                                    )
                                }
                            } else {
                                Icon(
                                    if (selected) tab.activeIcon else tab.icon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
            end = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
            top = innerPadding.calculateTopPadding() + 12.dp,
            bottom = innerPadding.calculateBottomPadding() + 16.dp
        )

        val enter: EnterTransition =
            if (animationsEnabled) fadeIn(tween(220)) else EnterTransition.None
        val exit: ExitTransition =
            if (animationsEnabled) fadeOut(tween(160)) else ExitTransition.None

        NavHost(
            navController = navController,
            startDestination = "home",
            enterTransition = { enter },
            exitTransition = { exit },
            modifier = Modifier.padding(0.dp)
        ) {
            composable("home") {
                HomeScreen(
                    state = home,
                    onAppClick = viewModel::openApp,
                    onRetry = viewModel::loadFeatured,
                    contentPadding = contentPadding
                )
            }
            composable("search") {
                SearchScreen(
                    state = search,
                    onQueryChange = viewModel::onQueryChange,
                    onToggleMarket = viewModel::toggleMarket,
                    onSelectAllMarkets = viewModel::selectAllMarkets,
                    onIconPicked = viewModel::searchByIcon,
                    onAppClick = viewModel::openApp,
                    contentPadding = contentPadding
                )
            }
            composable("downloads") {
                DownloadsScreen(
                    tasks = downloads,
                    scanStates = scanStates,
                    onInstall = viewModel::installTask,
                    onScan = viewModel::scanDownload,
                    onRemove = viewModel::removeTask,
                    contentPadding = contentPadding
                )
            }
            composable("settings") {
                SettingsScreen(
                    settings = settings,
                    onThemeChange = viewModel::setThemeMode,
                    onAutoScanChange = viewModel::setAutoScan,
                    onAnimationsChange = viewModel::setAnimations,
                    onWifiOnlyChange = viewModel::setWifiOnly,
                    onVtKeyChange = viewModel::setVtApiKey,
                    onDohChange = viewModel::setDohEnabled,
                    contentPadding = contentPadding
                )
            }
        }
    }

    selectedApp?.let { app ->
        AppDetailsSheet(
            app = app,
            onDownload = { a, option ->
                viewModel.download(a, option)
                viewModel.closeApp()
                navController.navigate("downloads") {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            onDismiss = viewModel::closeApp
        )
    }
}
