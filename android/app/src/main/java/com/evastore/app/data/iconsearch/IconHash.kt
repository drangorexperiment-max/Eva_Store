package com.evastore.app.data.iconsearch

import android.graphics.Bitmap
import android.graphics.Color

/**
 * Перцептивный dHash 8x8 (64 бита) для поиска визуально похожих иконок.
 * Быстрый, устойчивый к масштабированию и лёгкой перекраске.
 */
object IconHash {

    fun dHash(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
        var hash = 0L
        var bit = 0
        for (y in 0 until 8) {
            for (x in 0 until 8) {
                val left = luminance(scaled.getPixel(x, y))
                val right = luminance(scaled.getPixel(x + 1, y))
                if (left > right) hash = hash or (1L shl bit)
                bit++
            }
        }
        if (scaled != bitmap) scaled.recycle()
        return hash
    }

    /** Расстояние Хэмминга: 0 — идентичны, <= 10 — очень похожи. */
    fun distance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun luminance(pixel: Int): Int {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return (r * 299 + g * 587 + b * 114) / 1000
    }
}
