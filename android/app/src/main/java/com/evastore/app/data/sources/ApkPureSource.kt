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
 * APKPure: мобильное API (tapi.pureapk.com). Поиск сразу отдаёт прямую
 * ссылку на APK, размер и sha1. XAPK-контейнеры пропускаем — берём
 * только обычные APK, которые ставятся напрямую.
 */
object ApkPureSource : MarketSource {

    override val market = Market.APKPURE

    private val headers = mapOf(
        "User-Agent" to "APKPure/3.20.16 (Aegon)",
        "Ual-Access-Businessid" to "projecta"
    )

    override suspend fun search(query: String): List<StoreApp> {
        val body = Http.get(
            "https://tapi.pureapk.com/v3/search_query?hl=en&key=${query.urlEncode()}",
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
        val asset = info["asset"]?.jsonObject ?: return null
        // Только настоящие APK — XAPK требует особой установки.
        val type = asset["type"]?.jsonPrimitive?.contentOrNull
        if (type != null && !type.equals("APK", ignoreCase = true)) return null
        val url = asset["url"]?.jsonPrimitive?.contentOrNull ?: return null
        val size = asset["size"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        val sha1 = asset["sha1"]?.jsonPrimitive?.contentOrNull
        val versionName = info["version_name"]?.jsonPrimitive?.contentOrNull
        val icon = info["icon_url"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: info["icon"]?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
        val rating = info["review_stars"]?.jsonPrimitive?.doubleOrNull
        val downloads = info["download_count"]?.jsonPrimitive?.longOrNull
        val developer = info["developer"]?.jsonPrimitive?.contentOrNull

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
            options = listOf(
                DownloadOption(
                    market = Market.APKPURE,
                    url = url,
                    versionName = versionName,
                    sizeBytes = size,
                    fileName = "${pkg}_${versionName ?: "apkpure"}.apk",
                    sha1 = sha1
                )
            )
        )
    }
}
