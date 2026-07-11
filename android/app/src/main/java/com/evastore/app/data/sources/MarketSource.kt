package com.evastore.app.data.sources

import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import java.net.URLEncoder

/**
 * Подключаемый источник-маркет. Чтобы добавить новый маркет,
 * реализуйте интерфейс и зарегистрируйте его в CatalogRepository.
 */
interface MarketSource {
    val market: Market
    suspend fun search(query: String): List<StoreApp>
}

fun String.urlEncode(): String = URLEncoder.encode(this, "UTF-8")
