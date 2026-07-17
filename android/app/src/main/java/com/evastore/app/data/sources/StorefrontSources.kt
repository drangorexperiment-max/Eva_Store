package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.StoreApp

/**
 * Витринные маркеты удалены по решению пользователя (последней убрана
 * витрина GetApps). Объект оставлен как заглушка, чтобы не ломать сборку;
 * всегда возвращает пустой список.
 */
object StorefrontLinks {

    @Suppress("UNUSED_PARAMETER")
    fun verifiedOptionsFor(app: StoreApp): List<DownloadOption> = emptyList()
}
