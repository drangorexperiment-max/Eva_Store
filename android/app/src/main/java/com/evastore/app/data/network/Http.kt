package com.evastore.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object Http {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(url)
                .header("User-Agent", "EvaStore/1.0")
            headers.forEach { (k, v) -> builder.header(k, v) }
            client.newCall(builder.build()).await().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: $url")
                resp.body?.string() ?: throw IOException("Empty body: $url")
            }
        }

    /** Быстрая проверка существования страницы (HEAD, при неудаче — облегчённый GET). */
    suspend fun exists(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) EvaStore/1.0")
                .head()
                .build()
            client.newCall(request).await().use { resp ->
                when {
                    resp.isSuccessful -> true
                    // Некоторые серверы не любят HEAD — пробуем GET.
                    resp.code == 405 || resp.code == 403 -> {
                        val getReq = Request.Builder().url(url)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) EvaStore/1.0")
                            .build()
                        client.newCall(getReq).await().use { it.isSuccessful }
                    }
                    else -> false
                }
            }
        }.getOrDefault(false)
    }

    /**
     * Синхронный POST с настраиваемыми заголовками — для вызовов из
     * блокирующего кода (например, gplayapi работает синхронно).
     * Вызывать только с фонового потока.
     */
    fun postJsonBlocking(
        url: String,
        jsonBody: String,
        headers: Map<String, String> = emptyMap()
    ): String {
        val body = jsonBody.toRequestBody("application/json".toMediaType())
        val builder = Request.Builder().url(url)
            .header("User-Agent", "EvaStore/1.0")
            .post(body)
        headers.forEach { (k, v) -> builder.header(k, v) }
        client.newCall(builder.build()).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: $url")
            return resp.body?.string() ?: throw IOException("Empty body: $url")
        }
    }

    suspend fun postJson(url: String, jsonBody: String): String =
        withContext(Dispatchers.IO) {
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url)
                .header("User-Agent", "EvaStore/1.0")
                .post(body)
                .build()
            client.newCall(request).await().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: $url")
                resp.body?.string() ?: throw IOException("Empty body: $url")
            }
        }
}

suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) = cont.resume(response)
        override fun onFailure(call: Call, e: IOException) {
            if (!cont.isCancelled) cont.resumeWithException(e)
        }
    })
    cont.invokeOnCancellation { runCatching { cancel() } }
}
