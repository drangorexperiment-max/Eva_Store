package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp

/**
 * Витринные маркеты: у них нет открытого API для выдачи APK/IPA,
 * поэтому Eva Store формирует ссылку на карточку/поиск приложения
 * в официальном маркете по packageName или названию.
 */
object StorefrontLinks {

    fun optionsFor(app: StoreApp): List<DownloadOption> {
        val pkg = app.packageName ?: return emptyList()
        return listOf(
            DownloadOption(
                market = Market.GOOGLE_PLAY,
                url = "https://play.google.com/store/apps/details?id=$pkg"
            ),
            DownloadOption(
                market = Market.GETAPPS,
                url = "https://global.app.mi.com/details?id=$pkg"
            ),
            DownloadOption(
                market = Market.APP_STORE,
                url = "https://apps.apple.com/search?term=${app.name.urlEncode()}"
            )
        )
    }
}
