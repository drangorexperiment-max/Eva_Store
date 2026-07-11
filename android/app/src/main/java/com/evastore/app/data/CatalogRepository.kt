package com.evastore.app.data

import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.sources.FdroidSource
import com.evastore.app.data.sources.GithubSource
import com.evastore.app.data.sources.MarketSource
import com.evastore.app.data.sources.RustoreSource
import com.evastore.app.data.sources.StorefrontLinks
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Агрегатор каталога: параллельный поиск по всем подключённым маркетам,
 * дедупликация по packageName (одно приложение — несколько источников загрузки).
 */
class CatalogRepository {

    private val sources: List<MarketSource> = listOf(
        FdroidSource,
        RustoreSource,
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

    /** Популярное на главной: подборка запросов по категориям. */
    suspend fun featured(): List<StoreApp> = coroutineScope {
        val queries = listOf("browser", "messenger", "game", "music", "launcher")
        val results = queries.map { q ->
            async {
                runCatching { FdroidSource.search(q).take(6) }.getOrDefault(emptyList())
            }
        }.map { it.await() }
        mergeByPackage(results.flatten()).shuffled().take(24)
    }

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

        // Добавляем витринные ссылки (Google Play / GetApps / App Store)
        val merged = byPackage.values.map { app ->
            app.copy(
                options = (app.options + StorefrontLinks.optionsFor(app))
                    .distinctBy { it.market }
            )
        }
        return merged + noPackage
    }
}
