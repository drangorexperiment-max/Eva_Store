package com.evastore.app

import android.app.Application
import android.content.Intent
import android.os.Process
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.evastore.app.data.network.Http
import java.io.File
import kotlin.system.exitProcess

class EvaStoreApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // Заранее прогреваем DNS-кэш: первый поиск и загрузка иконок
        // не ждут медленные DoH-запросы.
        Http.prewarmDns()

        // Сохраняем текст любого необработанного краша в файл,
        // чтобы показать его пользователю при следующем запуске.
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            runCatching {
                File(filesDir, "last_crash.txt")
                    .writeText(android.util.Log.getStackTraceString(throwable))
            }
            // Показываем простой экран с текстом ошибки вместо молчаливого вылета.
            runCatching {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                }
                startActivity(intent)
            }
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Общий HTTP-клиент приложения: DNS-over-HTTPS и браузерный
            // User-Agent — иконки грузятся так же надёжно, как и APK.
            .okHttpClient(Http.client)
            // Кэшируем иконки, даже если CDN запрещает кэш заголовками.
            .respectCacheHeaders(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
