package com.evastore.app.data.sources

import com.aurora.gplayapi.data.models.AuthData
import com.aurora.gplayapi.helpers.AppDetailsHelper
import com.aurora.gplayapi.helpers.AuthHelper
import com.aurora.gplayapi.helpers.PurchaseHelper
import com.aurora.gplayapi.helpers.SearchHelper
import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

/**
 * Google Play через Aurora-подход: анонимный AuthToken от диспенсера
 * AuroraOSS + официальная библиотека gplayapi для поиска и получения
 * прямых ссылок на APK (base + splits).
 */
object GooglePlaySource : MarketSource {

    override val market = Market.GOOGLE_PLAY

    private const val DISPENSER_URL = "https://auroraoss.com/api/auth"

    @Volatile
    private var cachedAuth: AuthData? = null
    private var authAcquiredAt = 0L
    private val authMutex = Mutex()

    /** Токен Google живёт ~1 час; обновляем заранее. */
    private const val MAX_AUTH_AGE_MS = 45L * 60 * 1000

    private suspend fun auth(): AuthData? = authMutex.withLock {
        val existing = cachedAuth
        if (existing != null && System.currentTimeMillis() - authAcquiredAt < MAX_AUTH_AGE_MS) {
            return existing
        }
        val fresh = withContext(Dispatchers.IO) { fetchAnonymousAuth() }
        if (fresh != null) {
            cachedAuth = fresh
            authAcquiredAt = System.currentTimeMillis()
        }
        fresh
    }

    /** Получает анонимные email+authToken от диспенсера и строит AuthData. */
    private fun fetchAnonymousAuth(): AuthData? = runCatching {
        val response = Http.postJsonBlocking(
            DISPENSER_URL,
            "{}",
            mapOf("User-Agent" to "com.aurora.store-4.6.3-63")
        )
        val obj = Http.json.parseToJsonElement(response).jsonObject
        val email = obj["email"]?.jsonPrimitive?.contentOrNull ?: return null
        val token = obj["authToken"]?.jsonPrimitive?.contentOrNull ?: return null
        AuthHelper.build(
            email = email,
            token = token,
            tokenType = AuthHelper.Token.AUTH,
            isAnonymous = true,
            locale = Locale.getDefault()
        )
    }.getOrNull()

    override suspend fun search(query: String): List<StoreApp> = withContext(Dispatchers.IO) {
        val authData = auth() ?: return@withContext emptyList()
        runCatching {
            SearchHelper(authData).searchResults(query, "")
                .streamClusters.values
                .flatMap { it.clusterAppList }
                .distinctBy { it.packageName }
                .filter { it.isFree }
                .take(20)
                .map { app ->
                    StoreApp(
                        id = "gplay:${app.packageName}",
                        name = app.displayName,
                        packageName = app.packageName,
                        summary = app.shortDescription,
                        iconUrl = app.iconArtwork.url.ifBlank { null },
                        developer = app.developerName.ifBlank { null },
                        category = app.categoryName.ifBlank { null },
                        rating = app.rating.average.toDouble().takeIf { it > 0 },
                        downloads = app.installs.takeIf { it > 0 },
                        options = listOf(
                            DownloadOption(
                                market = Market.GOOGLE_PLAY,
                                url = "https://play.google.com/store/apps/details?id=${app.packageName}",
                                versionName = app.versionName.ifBlank { null },
                                sizeBytes = app.size.takeIf { it > 0 },
                                fileName = "${app.packageName}_${app.versionCode}.apk"
                            )
                        )
                    )
                }
        }.getOrDefault(emptyList())
    }

    /**
     * Прямая ссылка на base APK из Google Play.
     * Возвращает (url, fileName, size) или null.
     */
    suspend fun resolveApk(packageName: String): Triple<String, String, Long?>? =
        withContext(Dispatchers.IO) {
            val authData = auth() ?: return@withContext null
            runCatching {
                val app = AppDetailsHelper(authData).getAppByPackageName(packageName)
                if (!app.isFree) return@runCatching null
                val files = PurchaseHelper(authData)
                    .purchase(app.packageName, app.versionCode, app.offerType)
                val base = files.firstOrNull { it.name.endsWith(".apk") && !it.name.contains("config.") }
                    ?: files.firstOrNull()
                    ?: return@runCatching null
                Triple(
                    base.url,
                    "${app.packageName}_${app.versionCode}.apk",
                    base.size.takeIf { it > 0 }
                )
            }.getOrNull()
        }
}
