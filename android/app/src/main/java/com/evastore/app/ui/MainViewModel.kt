package com.evastore.app.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.imageLoader
import com.evastore.app.data.CatalogRepository
import com.evastore.app.data.download.ApkDownloader
import com.evastore.app.data.download.DownloadTask
import com.evastore.app.data.iconsearch.IconSearchEngine
import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.ScanState
import com.evastore.app.data.model.SourceType
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.scan.VirusTotalClient
import com.evastore.app.data.settings.EvaSettings
import com.evastore.app.data.settings.SettingsRepository
import com.evastore.app.data.sources.FdroidSource
import com.evastore.app.data.sources.GithubSource
import com.evastore.app.data.sources.GooglePlaySource
import com.evastore.app.data.sources.RustoreSource
import com.evastore.app.data.sources.StorefrontLinks
import com.evastore.app.ui.theme.ThemeMode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: List<StoreApp> = emptyList(),
    val iconSearchActive: Boolean = false,
    val iconMatches: List<IconSearchEngine.Match> = emptyList(),
    val selectedMarkets: Set<Market> = setOf(
        Market.GOOGLE_PLAY, Market.RUSTORE, Market.APKPURE,
        Market.APTOIDE, Market.FDROID, Market.GITHUB
    ),
    val error: String? = null
)

data class HomeUiState(
    val loading: Boolean = true,
    val featured: List<StoreApp> = emptyList(),
    val error: String? = null
)

@OptIn(FlowPreview::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val catalog = CatalogRepository()
    private val settingsRepo = SettingsRepository(app)
    val downloader = ApkDownloader(app)
    private val iconSearch = IconSearchEngine(app, app.imageLoader)

    val settings: StateFlow<EvaSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, EvaSettings())

    val downloads: StateFlow<List<DownloadTask>> = downloader.tasks

    private val _home = MutableStateFlow(HomeUiState())
    val home: StateFlow<HomeUiState> = _home

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search

    private val _scanStates = MutableStateFlow<Map<String, ScanState>>(emptyMap())
    val scanStates: StateFlow<Map<String, ScanState>> = _scanStates

    private val _selectedApp = MutableStateFlow<StoreApp?>(null)
    val selectedApp: StateFlow<StoreApp?> = _selectedApp

    private var searchJob: Job? = null

    // ВАЖНО: объявлено ДО init-блока — Kotlin инициализирует поля по порядку,
    // иначе в init поле ещё null и приложение падает при запуске (NPE).
    private val _queryFlow = MutableStateFlow("")

    init {
        loadFeatured()
        viewModelScope.launch {
            _queryFlow.debounce(450).collect { q ->
                if (q.length >= 2) performSearch(q)
            }
        }
    }

    fun loadFeatured() {
        viewModelScope.launch {
            _home.update { it.copy(loading = true, error = null) }
            runCatching { catalog.featured() }
                .onSuccess { apps -> _home.update { it.copy(loading = false, featured = apps) } }
                .onFailure { e ->
                    _home.update {
                        it.copy(loading = false, error = e.message ?: "Не удалось загрузить каталог")
                    }
                }
        }
    }

    fun onQueryChange(query: String) {
        _search.update { it.copy(query = query, iconSearchActive = false) }
        _queryFlow.value = query
        if (query.isBlank()) {
            searchJob?.cancel()
            _search.update { it.copy(results = emptyList(), loading = false, error = null) }
        }
    }

    fun toggleMarket(market: Market) {
        _search.update { state ->
            val newSet = if (market in state.selectedMarkets)
                state.selectedMarkets - market else state.selectedMarkets + market
            state.copy(selectedMarkets = newSet.ifEmpty { setOf(Market.FDROID) })
        }
        if (_search.value.query.length >= 2) performSearch(_search.value.query)
    }

    private fun performSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _search.update { it.copy(loading = true, error = null, iconSearchActive = false) }
            runCatching { catalog.search(query, _search.value.selectedMarkets) }
                .onSuccess { apps ->
                    _search.update { it.copy(loading = false, results = apps) }
                }
                .onFailure { e ->
                    if (e !is kotlinx.coroutines.CancellationException) {
                        _search.update {
                            it.copy(loading = false, error = e.message ?: "Ошибка поиска")
                        }
                    }
                }
        }
    }

    /** Поиск по изображению иконки: хэшируем картинку и ищем по каталогу. */
    fun searchByIcon(uri: Uri) {
        viewModelScope.launch {
            _search.update { it.copy(loading = true, iconSearchActive = true, error = null) }
            val hash = iconSearch.hashFromUri(uri)
            if (hash == null) {
                _search.update {
                    it.copy(loading = false, error = "Не удалось прочитать изображение")
                }
                return@launch
            }
            // Кандидаты: текущие результаты + подборка с главной
            val candidates = (_search.value.results + _home.value.featured).distinctBy { it.id }
            val pool = candidates.ifEmpty {
                runCatching { catalog.featured() }.getOrDefault(emptyList())
            }
            val matches = iconSearch.findSimilar(hash, pool)
            _search.update {
                it.copy(
                    loading = false,
                    iconMatches = matches,
                    error = if (matches.isEmpty())
                        "Похожих иконок не найдено. Сначала выполните текстовый поиск — это расширит базу сравнения." else null
                )
            }
        }
    }

    fun openApp(app: StoreApp) {
        _selectedApp.value = app
        // Лениво проверяем витрины (Google Play / App Store / GetApps):
        // добавляем только те, где приложение реально существует.
        viewModelScope.launch {
            val verified = runCatching { StorefrontLinks.verifiedOptionsFor(app) }
                .getOrDefault(emptyList())
            if (verified.isNotEmpty()) {
                _selectedApp.update { current ->
                    if (current?.id == app.id)
                        current.copy(
                            options = (current.options + verified).distinctBy { it.market }
                        )
                    else current
                }
            }
        }
    }

    fun closeApp() { _selectedApp.value = null }

    /** Скачивание выбранного варианта. DIRECT_APK — своя загрузка, STOREFRONT — переход. */
    fun download(app: StoreApp, option: DownloadOption) {
        when (option.market.type) {
            SourceType.STOREFRONT -> openExternal(option.url)
            SourceType.DIRECT_APK -> viewModelScope.launch {
                val resolved: Triple<String, String, Long?>? = when (option.market) {
                    Market.FDROID -> app.packageName?.let { pkg ->
                        runCatching {
                            val (url, name) = FdroidSource.resolveApkUrl(pkg)
                            Triple(url, name, null as Long?)
                        }.getOrNull()
                    }
                    Market.GITHUB -> runCatching {
                        GithubSource.resolveApkUrl(option.url)
                            ?.let { Triple(it.first, it.second, it.third as Long?) }
                    }.getOrNull()
                    Market.RUSTORE -> option.appId?.let { appId ->
                        runCatching {
                            RustoreSource.resolveApkUrl(appId)?.let { url ->
                                val name = option.fileName ?: "${app.packageName ?: "rustore"}.apk"
                                Triple(url, name, option.sizeBytes)
                            }
                        }.getOrNull()
                    }
                    // Aptoide и APKPure отдают прямую ссылку сразу в поиске.
                    Market.APTOIDE, Market.APKPURE -> Triple(
                        option.url,
                        option.fileName ?: "${app.packageName ?: option.market.name.lowercase()}.apk",
                        option.sizeBytes
                    )
                    // Google Play: получаем прямую ссылку через анонимный
                    // аккаунт (Aurora-подход). При неудаче — откроем витрину.
                    Market.GOOGLE_PLAY -> app.packageName?.let { pkg ->
                        runCatching { GooglePlaySource.resolveApk(pkg) }.getOrNull()
                    }
                    else -> null
                }

                if (resolved == null) {
                    openExternal(option.url)
                    return@launch
                }

                val (url, fileName) = resolved
                val taskId = app.id
                downloader.enqueue(
                    id = taskId,
                    appName = app.name,
                    fileName = fileName,
                    url = url,
                    iconUrl = app.iconUrl,
                    marketLabel = option.market.label
                ) { file ->
                    if (settings.value.autoScanDownloads && effectiveVtKey().isNotBlank()) {
                        scanFile(taskId, file)
                    }
                }
            }
        }
    }

    fun scanDownload(task: DownloadTask) {
        val path = task.filePath ?: return
        viewModelScope.launch { scanFile(task.id, File(path)) }
    }

    /** Ключ из настроек, а если пуст — встроенный из сборки (GitHub-секрет). */
    private fun effectiveVtKey(): String =
        settings.value.virusTotalApiKey.ifBlank { com.evastore.app.BuildConfig.VT_API_KEY }

    private suspend fun scanFile(taskId: String, file: File) {
        val key = effectiveVtKey()
        if (key.isBlank()) {
            // Без ключа: открываем публичный отчёт VirusTotal по SHA-256 в браузере.
            val sha = runCatching { fileSha256(file) }.getOrNull()
            if (sha != null) {
                openExternal("https://www.virustotal.com/gui/file/$sha")
                _scanStates.update {
                    it + (taskId to ScanState.Error(
                        "Отчёт открыт в браузере. Для проверки внутри приложения добавьте API-ключ в Настройках."
                    ))
                }
            } else {
                _scanStates.update {
                    it + (taskId to ScanState.Error("Добавьте API-ключ VirusTotal в Настройках"))
                }
            }
            return
        }
        downloader.setScanning(taskId, true)
        _scanStates.update { it + (taskId to ScanState.Uploading) }
        runCatching {
            VirusTotalClient(key).scanFile(file) { phase ->
                _scanStates.update { it + (taskId to ScanState.Analyzing) }
            }
        }.onSuccess { result ->
            _scanStates.update { it + (taskId to ScanState.Done(result)) }
        }.onFailure { e ->
            _scanStates.update { it + (taskId to ScanState.Error(e.message ?: "Ошибка сканирования")) }
        }
        downloader.setScanning(taskId, false)
    }

    fun installTask(task: DownloadTask) {
        task.filePath?.let { downloader.installApk(it) }
    }

    fun removeTask(task: DownloadTask) {
        downloader.remove(task.id)
        _scanStates.update { it - task.id }
    }

    // Настройки
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setAutoScan(v: Boolean) = viewModelScope.launch { settingsRepo.setAutoScan(v) }
    fun setAnimations(v: Boolean) = viewModelScope.launch { settingsRepo.setAnimations(v) }
    fun setWifiOnly(v: Boolean) = viewModelScope.launch { settingsRepo.setWifiOnly(v) }
    fun setVtApiKey(key: String) = viewModelScope.launch { settingsRepo.setVirusTotalApiKey(key) }

    private fun openExternal(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        }
    }

    private fun fileSha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
