package com.evastore.app.data.iconsearch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.evastore.app.data.model.StoreApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toBitmap

/**
 * Поиск приложений по изображению иконки: считаем dHash загруженной
 * пользователем картинки и сравниваем с хэшами иконок каталога.
 */
class IconSearchEngine(
    private val context: Context,
    private val imageLoader: ImageLoader
) {

    data class Match(val app: StoreApp, val distance: Int)

    suspend fun hashFromUri(uri: Uri): Long? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bmp = BitmapFactory.decodeStream(stream) ?: return@runCatching null
                val hash = IconHash.dHash(bmp)
                bmp.recycle()
                hash
            }
        }.getOrNull()
    }

    /** Сравнивает хэш запроса с иконками кандидатов; порог 14 из 64 бит. */
    suspend fun findSimilar(
        queryHash: Long,
        candidates: List<StoreApp>,
        threshold: Int = 14
    ): List<Match> = coroutineScope {
        candidates
            .filter { it.iconUrl != null }
            .map { app ->
                async(Dispatchers.IO) {
                    val hash = hashForUrl(app.iconUrl!!) ?: return@async null
                    val dist = IconHash.distance(queryHash, hash)
                    if (dist <= threshold) Match(app, dist) else null
                }
            }
            .mapNotNull { it.await() }
            .sortedBy { it.distance }
    }

    private val hashCache = HashMap<String, Long>()

    private suspend fun hashForUrl(url: String): Long? {
        hashCache[url]?.let { return it }
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .size(64)
                .build()
            val result = imageLoader.execute(request) as? SuccessResult ?: return null
            val bitmap: Bitmap = result.drawable.toBitmap()
            val hash = IconHash.dHash(bitmap)
            hashCache[url] = hash
            hash
        }.getOrNull()
    }
}
