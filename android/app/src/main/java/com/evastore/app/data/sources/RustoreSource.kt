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
            val appId = obj["appId"]?.jsonPrimitive?.content?.toLongOrNull()
            // Платные приложения не отдают прямой APK — их ведём на витрину.
            val price = obj["price"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val versionCode = obj["versionCode"]?.jsonPrimitive?.content
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
                        url = "https://www.rustore.ru/catalog/app/$pkg",
                        appId = if (price == 0) appId else null,
                        fileName = "${pkg}_${versionCode ?: "rustore"}.apk"
                    )
                )
            )
        }
    }

    @Serializable
    private data class DownloadLinkResponse(val body: LinkBody? = null)

    @Serializable
    private data class LinkBody(val apkUrl: String? = null, val versionCode: Long? = null)

    /**
     * Получает прямую ссылку на файл (zip-контейнер с APK внутри) по appId.
     * Возвращает пару: URL и признак того, что это zip-архив.
     */
    suspend fun resolveApkUrl(appId: Long): String? {
        val resp = Http.postJson(
            "https://backapi.rustore.ru/applicationData/download-link",
            """{"appId":$appId,"firstInstall":true}"""
        )
        val parsed = Http.json.decodeFromString<DownloadLinkResponse>(resp)
        return parsed.body?.apkUrl
    }
}
