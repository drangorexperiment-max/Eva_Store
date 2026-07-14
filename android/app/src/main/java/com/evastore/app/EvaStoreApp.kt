package com.evastore.app

import android.app.Application
import android.content.Intent
import android.os.Process
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File
import kotlin.system.exitProcess

class EvaStoreApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

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
