package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
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
            val versionName = obj["versionName"]?.jsonPrimitive?.contentOrNull
            val fileSize = obj["fileSize"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
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
                        versionName = versionName,
                        sizeBytes = fileSize,
                        fileName = "${pkg}_${versionCode ?: "rustore"}.apk"
                    )
                )
            )
        }
    }

    /**
     * Получает прямую ссылку на единый APK по appId через v2-эндпоинт
     * с флагом withoutSplits=true — тот же метод, что использует
     * веб-загрузчик rustore-downloader. Возвращает универсальный apk,
     * а не набор split-частей, поэтому файл ставится напрямую.
     */
    suspend fun resolveApkUrl(appId: Long): String? {
        val requestBody = """
            {
              "appId": $appId,
              "firstInstall": true,
              "withoutSplits": true,
              "mobileServices": ["GMS","HMS"],
              "supportedAbis": ["arm64-v8a","armeabi-v7a","x86_64","x86","armeabi"],
              "screenDensity": 480,
              "supportedLocales": ["ru_RU","en_US"],
              "sdkVersion": 34,
              "signatureFingerprint": null
            }
        """.trimIndent()

        // Сначала пробуем v2 (единый apk), при неудаче — старый v1.
        val v2 = runCatching {
            Http.postJson("https://backapi.rustore.ru/applicationData/v2/download-link", requestBody)
        }.getOrNull()
        parseApkUrl(v2)?.let { return it }

        val v1 = runCatching {
            Http.postJson(
                "https://backapi.rustore.ru/applicationData/download-link",
                """{"appId":$appId,"firstInstall":true}"""
            )
        }.getOrNull()
        return parseApkUrl(v1)
    }

    private fun parseApkUrl(resp: String?): String? {
        if (resp.isNullOrBlank()) return null
        val body = runCatching {
            Http.json.parseToJsonElement(resp).jsonObject["body"]?.jsonObject
        }.getOrNull() ?: return null
        // Разные варианты полей в v1/v2.
        body["apkUrl"]?.jsonPrimitive?.content?.let { if (it.isNotBlank()) return it }
        val urls = body["downloadUrls"]?.jsonArray ?: body["apkUrls"]?.jsonArray
        urls?.firstOrNull()?.let { el ->
            (el as? JsonObject)?.get("url")?.jsonPrimitive?.content?.let { return it }
            el.jsonPrimitive.contentOrNull?.let { return it }
        }
        return null
    }

    /** Размер файла и версия из подробной карточки. */
    suspend fun fetchDetails(packageName: String): Pair<Long?, String?>? {
        val resp = runCatching {
            Http.get("https://backapi.rustore.ru/applicationData/overallInfo/$packageName")
        }.getOrNull() ?: return null
        val body = runCatching {
            Http.json.parseToJsonElement(resp).jsonObject["body"]?.jsonObject
        }.getOrNull() ?: return null
        val size = body["fileSize"]?.jsonPrimitive?.contentOrNull?.toLongOrNull()
        val version = body["versionName"]?.jsonPrimitive?.contentOrNull
        return size to version
    }
}
