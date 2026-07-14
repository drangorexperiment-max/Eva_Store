package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Aptoide: открытое webservices-API (ws75). Отдаёт прямую ссылку на APK
 * и размер файла — приложения скачиваются напрямую в Eva Store.
 */
object AptoideSource : MarketSource {

    override val market = Market.APTOIDE

    override suspend fun search(query: String): List<StoreApp> {
        val body = Http.get(
            "https://ws75.aptoide.com/api/7/apps/search/query=${query.urlEncode()}/limit=25"
        )
        val root = Http.json.parseToJsonElement(body).jsonObject
        val list = root["datalist"]?.jsonObject?.get("list") as? JsonArray ?: return emptyList()

        return list.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val pkg = obj["package"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: pkg
            val file = obj["file"]?.jsonObject
            val apkUrl = file?.get("path")?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val versionName = file["vername"]?.jsonPrimitive?.contentOrNull
            val md5 = file["md5sum"]?.jsonPrimitive?.contentOrNull
            val size = obj["size"]?.jsonPrimitive?.longOrNull
            val icon = obj["icon"]?.jsonPrimitive?.contentOrNull
            val developer = obj["developer"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
            val downloads = obj["stats"]?.jsonObject?.get("downloads")?.jsonPrimitive?.longOrNull
            val rating = obj["stats"]?.jsonObject?.get("rating")?.jsonObject
                ?.get("avg")?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()

            StoreApp(
                id = "aptoide:$pkg",
                name = name,
                packageName = pkg,
                summary = "",
                iconUrl = icon,
                developer = developer,
                category = null,
                rating = rating,
                downloads = downloads,
                options = listOf(
                    DownloadOption(
                        market = Market.APTOIDE,
                        url = apkUrl,
                        versionName = versionName,
                        sizeBytes = size,
                        fileName = "${pkg}_${versionName ?: "aptoide"}.apk",
                        sha1 = md5
                    )
                )
            )
        }
    }
}
