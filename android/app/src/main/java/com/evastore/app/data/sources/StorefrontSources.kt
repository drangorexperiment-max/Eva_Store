package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http

/**
 * Витринные маркеты: у них нет открытого API для выдачи APK.
 * Ссылки добавляются ТОЛЬКО после проверки, что приложение
 * действительно существует в этом маркете.
 *
 * Сейчас единственная витрина — GetApps (Xiaomi). Google Play стал
 * прямым источником (GooglePlaySource), App Store убран по решению
 * пользователя.
 */
object StorefrontLinks {

    /**
     * Проверяет наличие приложения в витринных маркетах и возвращает
     * только реально существующие варианты. Вызывается лениво при
     * открытии карточки, чтобы не спамить сеть при поиске.
     */
    suspend fun verifiedOptionsFor(app: StoreApp): List<DownloadOption> {
        val pkg = app.packageName ?: return emptyList()
        val url = "https://global.app.mi.com/details?id=$pkg"
        return if (Http.exists(url)) listOf(DownloadOption(Market.GETAPPS, url))
        else emptyList()
    }
}
