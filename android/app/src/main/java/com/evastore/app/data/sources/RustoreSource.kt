package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * RuStore: публичное API витрины. Поиск приложений; переход на страницу
 * приложения в RuStore для скачивания (или прямой APK, если доступен).
 */
object RustoreSource : MarketSource {

    override val market = Market.RUSTORE

    override suspend fun search(query: String): List<StoreApp> {
        val body = Http.get(
            "https://backapi.rustore.ru/applicationData/apps?query=${query.urlEncode()}&pageNumber=0&pageSize=20"
        )
        val root = Http.json.parseToJsonElement(body).jsonObject
        val content = root["body"]?.jsonObject?.get("content")?.jsonArray ?: return emptyList()

        return content.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val pkg = obj["packageName"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val name = obj["appName"]?.jsonPrimitive?.content ?: pkg
            StoreApp(
                id = "rustore:$pkg",
                name = name,
                packageName = pkg,
                summary = obj["shortDescription"]?.jsonPrimitive?.content.orEmpty(),
                iconUrl = obj["iconUrl"]?.jsonPrimitive?.content,
                developer = obj["companyName"]?.jsonPrimitive?.content,
                category = obj["categoryName"]?.jsonPrimitive?.content,
                options = listOf(
                    DownloadOption(
                        market = Market.RUSTORE,
                        url = "https://www.rustore.ru/catalog/app/$pkg"
                    )
                )
            )
        }
    }
}
