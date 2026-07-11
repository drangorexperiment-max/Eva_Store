package com.evastore.app.data.model

/**
 * Маркет-источник приложений. type определяет возможности:
 * DIRECT_APK — можно скачать APK прямо через Eva Store;
 * STOREFRONT — витрина: показываем карточку и ведём в официальный источник.
 */
enum class SourceType { DIRECT_APK, STOREFRONT }

enum class Market(
    val label: String,
    val type: SourceType,
    val brandColor: Long
) {
    FDROID("F-Droid", SourceType.DIRECT_APK, 0xFF1976D2),
    GITHUB("GitHub", SourceType.DIRECT_APK, 0xFF24292F),
    RUSTORE("RuStore", SourceType.DIRECT_APK, 0xFF0077FF),
    GETAPPS("GetApps", SourceType.STOREFRONT, 0xFFFF6900),
    GOOGLE_PLAY("Google Play", SourceType.STOREFRONT, 0xFF34A853),
    APP_STORE("App Store", SourceType.STOREFRONT, 0xFF0D84FF)
}

/** Один вариант скачивания у конкретного маркета. */
data class DownloadOption(
    val market: Market,
    /** Прямая ссылка на APK (для DIRECT_APK) или ссылка на страницу (для STOREFRONT). */
    val url: String,
    val versionName: String? = null,
    val sizeBytes: Long? = null,
    val fileName: String? = null
)

/** Приложение в каталоге Eva Store. */
data class StoreApp(
    val id: String,
    val name: String,
    val packageName: String?,
    val summary: String,
    val iconUrl: String?,
    val developer: String?,
    val category: String?,
    val options: List<DownloadOption>
) {
    val primaryMarket: Market get() = options.first().market
}

/** Результат VirusTotal-скана. */
data class ScanResult(
    val malicious: Int,
    val suspicious: Int,
    val harmless: Int,
    val undetected: Int,
    val total: Int,
    val permalink: String?
) {
    val isClean: Boolean get() = malicious == 0 && suspicious == 0
}

sealed interface ScanState {
    data object Idle : ScanState
    data object Uploading : ScanState
    data object Analyzing : ScanState
    data class Done(val result: ScanResult) : ScanState
    data class Error(val message: String) : ScanState
}
