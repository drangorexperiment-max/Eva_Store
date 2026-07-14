package com.evastore.app.data

import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.sources.ApkPureSource
import com.evastore.app.data.sources.AptoideSource
import com.evastore.app.data.sources.FdroidSource
import com.evastore.app.data.sources.GithubSource
import com.evastore.app.data.sources.GooglePlaySource
import com.evastore.app.data.sources.MarketSource
import com.evastore.app.data.sources.RustoreSource
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Агрегатор каталога: параллельный поиск по всем подключённым маркетам,
 * дедупликация по packageName (одно приложение — несколько источников загрузки).
 */
class CatalogRepository {

    private val sources: List<MarketSource> = listOf(
        GooglePlaySource,
        RustoreSource,
        ApkPureSource,
        AptoideSource,
        FdroidSource,
        GithubSource
    )

    val enabledMarkets: List<Market> get() = sources.map { it.market }

    suspend fun search(
        query: String,
        markets: Set<Market> = enabledMarkets.toSet()
    ): List<StoreApp> = coroutineScope {
        val active = sources.filter { it.market in markets }
        val results = active.map { source ->
            async {
                runCatching { source.search(query) }.getOrDefault(emptyList())
            }
        }.map { it.await() }

        mergeByPackage(results.flatten())
    }

    /** Популярное на главной: подборка запросов по категориям из разных маркетов. */
    suspend fun featured(): List<StoreApp> = coroutineScope {
        val rustoreQueries = listOf("игра", "мессенджер", "музыка", "браузер")
        val apkpureQueries = listOf("popular", "game")
        val fdroidQueries = listOf("browser", "player")

        val jobs = buildList {
            rustoreQueries.forEach { q ->
                add(async { runCatching { RustoreSource.search(q).take(6) }.getOrDefault(emptyList()) })
            }
            apkpureQueries.forEach { q ->
                add(async { runCatching { ApkPureSource.search(q).take(8) }.getOrDefault(emptyList()) })
            }
            fdroidQueries.forEach { q ->
                add(async { runCatching { FdroidSource.search(q).take(4) }.getOrDefault(emptyList()) })
            }
        }
        val all = jobs.map { it.await() }.flatten()
        mergeByPackage(all).shuffled().take(36)
    }

    /** Категории для главной с готовыми запросами. */
    val categories: List<Pair<String, String>> = listOf(
        "Игры" to "game",
        "Мессенджеры" to "messenger",
        "Музыка" to "music",
        "Браузеры" to "browser",
        "Инструменты" to "tools",
        "Камера" to "camera"
    )

    suspend fun byQuery(query: String): List<StoreApp> = search(query)

    private fun mergeByPackage(apps: List<StoreApp>): List<StoreApp> {
        val byPackage = LinkedHashMap<String, StoreApp>()
        val noPackage = mutableListOf<StoreApp>()

        for (app in apps) {
            val pkg = app.packageName
            if (pkg == null) {
                noPackage += app
                continue
            }
            val existing = byPackage[pkg]
            byPackage[pkg] = if (existing == null) app else existing.copy(
                iconUrl = existing.iconUrl ?: app.iconUrl,
                summary = existing.summary.ifBlank { app.summary },
                developer = existing.developer ?: app.developer,
                category = existing.category ?: app.category,
                options = (existing.options + app.options).distinctBy { it.market }
            )
        }

        // Витринные ссылки (Google Play / GetApps / App Store) добавляются
        // лениво при открытии карточки — после проверки, что приложение
        // реально существует в этих маркетах (StorefrontLinks.verifiedOptionsFor).
        return byPackage.values.toList() + noPackage
    }
}
