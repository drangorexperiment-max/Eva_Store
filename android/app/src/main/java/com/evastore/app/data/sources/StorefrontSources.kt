package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Витринные маркеты: у них нет открытого API для выдачи APK/IPA.
 * Ссылки добавляются ТОЛЬКО после проверки, что приложение
 * действительно существует в этом маркете.
 */
object StorefrontLinks {

    /**
     * Проверяет наличие приложения в витринных маркетах и возвращает
     * только реально существующие варианты. Вызывается лениво при
     * открытии карточки, чтобы не спамить сеть при поиске.
     */
    suspend fun verifiedOptionsFor(app: StoreApp): List<DownloadOption> = coroutineScope {
        val pkg = app.packageName ?: return@coroutineScope emptyList()

        val playJob = async {
            val url = "https://play.google.com/store/apps/details?id=$pkg"
            if (Http.exists(url)) DownloadOption(Market.GOOGLE_PLAY, url) else null
        }
        val appStoreJob = async {
            // iTunes Lookup API — официальный способ проверить наличие по bundleId.
            runCatching {
                val resp = Http.get("https://itunes.apple.com/lookup?bundleId=$pkg")
                val count = Http.json.parseToJsonElement(resp)
                    .jsonObject["resultCount"]?.jsonPrimitive?.intOrNull ?: 0
                if (count > 0)
                    DownloadOption(
                        Market.APP_STORE,
                        "https://apps.apple.com/app/id" + extractTrackId(resp)
                    )
                else null
            }.getOrNull()
        }
        val getAppsJob = async {
            val url = "https://global.app.mi.com/details?id=$pkg"
            if (Http.exists(url)) DownloadOption(Market.GETAPPS, url) else null
        }

        listOfNotNull(playJob.await(), appStoreJob.await(), getAppsJob.await())
    }

    private fun extractTrackId(lookupResponse: String): String =
        runCatching {
            val results = Http.json.parseToJsonElement(lookupResponse)
                .jsonObject["results"] as? kotlinx.serialization.json.JsonArray
            results?.firstOrNull()?.jsonObject?.get("trackId")?.jsonPrimitive?.content
        }.getOrNull() ?: ""
}
