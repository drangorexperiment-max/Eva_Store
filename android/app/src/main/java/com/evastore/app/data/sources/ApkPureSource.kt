package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * APKPure. Поиск — мобильное API (tapi.pureapk.com), метаданные и размер.
 * Скачивание — эндпоинт d.apkpure.com/b/APK/<pkg>, который отдаёт
 * настоящий универсальный APK (ссылки из поиска ведут на XAPK и
 * возвращают 405 при прямом GET — из-за этого раньше «скачивался»
 * мусорный файл и установка падала).
 */
object ApkPureSource : MarketSource {

    override val market = Market.APKPURE

    private val headers = mapOf(
        "User-Agent" to "APKPure/3.20.16 (Aegon)",
        "Ual-Access-Businessid" to "projecta"
    )

    /** Прямая ссылка на актуальный APK пакета (302 → CDN data.winudf.com). */
    fun apkUrl(packageName: String): String =
        "https://d.apkpure.com/b/APK/$packageName?version=latest"

    override suspend fun search(query: String): List<StoreApp> {
        val body = Http.get(
            "https://tapi.pureapk.com/v3/search_query?hl=ru&key=${query.urlEncode()}",
            headers
        )
        val root = Http.json.parseToJsonElement(body).jsonObject
        val cmsList = root["cms_list"] as? JsonArray ?: return emptyList()

        val apps = mutableListOf<StoreApp>()
        for (cms in cmsList) {
            val items = (cms as? JsonObject)?.get("item_list") as? JsonArray ?: continue
            for (item in items) {
                val info = (item as? JsonObject)?.get("app_info") as? JsonObject ?: continue
                val app = parseApp(info) ?: continue
                apps += app
            }
        }
        return apps.distinctBy { it.packageName }.take(25)
    }

    private fun parseApp(info: JsonObject): StoreApp? {
        val pkg = info["package_name"]?.jsonPrimitive?.contentOrNull ?: return null
        val title = info["title"]?.jsonPrimitive?.contentOrNull ?: pkg
        val asset = info["asset"]?.jsonObject
        val size = asset?.get("size")?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        val versionName = info["version_name"]?.jsonPrimitive?.contentOrNull
        val icon = info["icon_url"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: info["icon"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        val rating = info["review_stars"]?.jsonPrimitive?.doubleOrNull
        val downloads = info["download_count"]?.jsonPrimitive?.longOrNull
        val developer = info["developer"]?.jsonPrimitive?.contentOrNull
        val screenshots = (info["screenshots"] as? JsonArray)
            ?.mapNotNull { el ->
                (el as? JsonObject)?.get("url")?.jsonPrimitive?.contentOrNull
                    ?: el.jsonPrimitive.contentOrNull
            }
            ?.filter { it.startsWith("http") }
            .orEmpty()

        return StoreApp(
            id = "apkpure:$pkg",
            name = title,
            packageName = pkg,
            summary = info["description_short"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            iconUrl = icon,
            developer = developer,
            category = info["category_name"]?.jsonPrimitive?.contentOrNull,
            rating = rating,
            downloads = downloads,
            screenshots = screenshots,
            options = listOf(
                DownloadOption(
                    market = Market.APKPURE,
                    // Стабильный эндпоинт вместо подписанных XAPK-ссылок из поиска.
                    url = apkUrl(pkg),
                    versionName = versionName,
                    sizeBytes = size,
                    fileName = "${pkg}_${versionName ?: "apkpure"}.apk"
                )
            )
        )
    }
}
