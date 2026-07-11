package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.serialization.Serializable

/**
 * F-Droid: открытый каталог FOSS-приложений с прямыми APK.
 * Поиск: search.f-droid.org, скачивание: f-droid.org/repo/<pkg>_<code>.apk
 */
object FdroidSource : MarketSource {

    override val market = Market.FDROID

    @Serializable
    private data class SearchResponse(val results: List<Result> = emptyList())

    @Serializable
    private data class Result(
        val package_name: String = "",
        val name: String = "",
        val summary: String = "",
        val icon: String? = null
    )

    override suspend fun search(query: String): List<StoreApp> {
        val body = Http.get(
            "https://search.f-droid.org/api/search_apps?q=${query.urlEncode()}&lang=en"
        )
        val parsed = Http.json.decodeFromString<SearchResponse>(body)
        return parsed.results.take(30).map { r ->
            val icon = r.icon?.let { ic ->
                if (ic.startsWith("http")) ic
                else "https://f-droid.org/repo/icons-640/$ic"
            }
            StoreApp(
                id = "fdroid:${r.package_name}",
                name = r.name.ifBlank { r.package_name },
                packageName = r.package_name,
                summary = r.summary,
                iconUrl = icon,
                developer = null,
                category = null,
                options = listOf(
                    DownloadOption(
                        market = Market.FDROID,
                        // Страница пакета отдаёт актуальный APK; резолвим при скачивании.
                        url = "https://f-droid.org/api/v1/packages/${r.package_name}"
                    )
                )
            )
        }
    }

    @Serializable
    private data class PackageInfo(
        val packageName: String = "",
        val suggestedVersionCode: Long = 0,
        val packages: List<Pkg> = emptyList()
    )

    @Serializable
    private data class Pkg(val versionName: String = "", val versionCode: Long = 0)

    /** Резолвит прямую ссылку на последний APK пакета. */
    suspend fun resolveApkUrl(packageName: String): Pair<String, String> {
        val body = Http.get("https://f-droid.org/api/v1/packages/$packageName")
        val info = Http.json.decodeFromString<PackageInfo>(body)
        val version = info.packages.firstOrNull { it.versionCode == info.suggestedVersionCode }
            ?: info.packages.firstOrNull()
        val code = version?.versionCode ?: info.suggestedVersionCode
        return "https://f-droid.org/repo/${packageName}_$code.apk" to
            "${packageName}_$code.apk"
    }
}
