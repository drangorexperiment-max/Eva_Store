package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Google Play — прямое скачивание APK через зеркало каталога Google Play
 * (API pureapk + CDN winudf.com). Диспенсер анонимных аккаунтов Aurora
 * блокируется Cloudflare для большинства IP в РФ, поэтому вместо gplayapi
 * используем зеркало: оно отдаёт те же APK приложений из каталога Google
 * Play — без аккаунта, без VPN и без редиректа в маркет.
 */
object GooglePlaySource : MarketSource {

    override val market = Market.GOOGLE_PLAY

    private const val SEARCH_URL = "https://tapi.pureapk.com/v3/search_query"

    /** Заголовки API зеркала: без них сервер отвечает ошибкой. */
    val apiHeaders = mapOf(
        "Ual-Access-Businessid" to "projecta",
        "User-Agent" to "APKPure/3.20.16 (Aegon)"
    )

    override suspend fun search(query: String): List<StoreApp> {
        val url = "$SEARCH_URL?hl=ru&key=${query.urlEncode()}"
        val body = runCatching { Http.get(url, apiHeaders) }.getOrNull() ?: return emptyList()

        return runCatching {
            val root = Http.json.parseToJsonElement(body).jsonObject
            val results = mutableListOf<StoreApp>()

            root["cms_list"]?.jsonArray?.forEach { cmsEl ->
                cmsEl.jsonObject["item_list"]?.jsonArray?.forEach itemLoop@{ itemEl ->
                    val info = itemEl.jsonObject["app_info"]?.jsonObject ?: return@itemLoop
                    val pkg = info["package_name"]?.jsonPrimitive?.contentOrNull ?: return@itemLoop
                    val title = info["title"]?.jsonPrimitive?.contentOrNull ?: return@itemLoop
                    val asset = info["asset"]?.jsonObject ?: return@itemLoop
                    val type = asset["type"]?.jsonPrimitive?.contentOrNull ?: "APK"
                    // XAPK (split-пакеты) наш установщик не поддерживает — пропускаем.
                    if (!type.equals("APK", ignoreCase = true)) return@itemLoop
                    // Подписанные ссылки из поиска отдают 405 — используем
                    // стабильный эндпоинт, отдающий актуальный APK по пакету.
                    val apkUrl = "https://d.apkpure.com/b/APK/$pkg?version=latest"

                    // Иконка встречается в трёх форматах ответа — пробуем все.
                    val icon = info["icon_url"]?.jsonPrimitive?.contentOrNull
                        ?.takeIf { it.isNotBlank() }
                        ?: info["icon"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                        ?: info["icon"]?.jsonObject
                            ?.get("thumbnail")?.jsonObject
                            ?.get("url")?.jsonPrimitive?.contentOrNull

                    val screenshots = info["screenshots"]?.jsonArray?.mapNotNull { s ->
                        runCatching {
                            s.jsonObject["url"]?.jsonPrimitive?.contentOrNull
                                ?: s.jsonObject["thumbnail"]?.jsonObject
                                    ?.get("url")?.jsonPrimitive?.contentOrNull
                        }.getOrNull() ?: runCatching {
                            s.jsonPrimitive.contentOrNull
                        }.getOrNull()
                    }?.filter { it.startsWith("http") }.orEmpty()

                    val rating = info["review_stars"]?.jsonPrimitive?.doubleOrNull
                        ?: info["stats"]?.jsonObject?.get("rating")?.jsonObject
                            ?.get("average")?.jsonPrimitive?.doubleOrNull
                    val downloads = info["download_count"]?.jsonPrimitive?.longOrNull
                        ?: info["stats"]?.jsonObject?.get("download")?.jsonObject
                            ?.get("download_num")?.jsonPrimitive?.longOrNull

                    results += StoreApp(
                        id = "gplay:$pkg",
                        name = title,
                        packageName = pkg,
                        summary = info["description_short"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        iconUrl = icon,
                        developer = info["developer"]?.jsonPrimitive?.contentOrNull,
                        category = info["category_name"]?.jsonPrimitive?.contentOrNull,
                        rating = rating?.takeIf { it > 0 },
                        downloads = downloads?.takeIf { it > 0 },
                        screenshots = screenshots,
                        options = listOf(
                            DownloadOption(
                                market = Market.GOOGLE_PLAY,
                                url = apkUrl,
                                versionName = info["version_name"]?.jsonPrimitive?.contentOrNull,
                                sizeBytes = asset["size"]?.jsonPrimitive?.longOrNull?.takeIf { it > 0 },
                                fileName = "$pkg.apk"
                            )
                        )
                    )
                }
            }
            results.distinctBy { it.packageName }.take(20)
        }.getOrDefault(emptyList())
    }
}
