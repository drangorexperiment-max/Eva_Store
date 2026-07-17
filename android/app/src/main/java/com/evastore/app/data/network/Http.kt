package com.evastore.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * DNS с обходом блокировок: сначала DNS-over-HTTPS (Google, потом Cloudflare —
 * оба с захардкоженными IP, чтобы не зависеть от DNS провайдера),
 * при неудаче — системный DNS. Это снимает главный класс блокировок
 * в РФ: подмену DNS-ответов провайдером.
 */
private object SmartDns : Dns {

    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    private val googleDoh: DnsOverHttps by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://dns.google/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("8.8.8.8"),
                InetAddress.getByName("8.8.4.4")
            )
            .build()
    }

    private val cloudflareDoh: DnsOverHttps by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url("https://cloudflare-dns.com/dns-query".toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("1.1.1.1"),
                InetAddress.getByName("1.0.0.1")
            )
            .build()
    }

    /** Кэш успешных ответов, чтобы не дёргать DoH на каждый запрос. */
    private val cache = java.util.concurrent.ConcurrentHashMap<String, List<InetAddress>>()

    @Volatile
    var enabled: Boolean = true

    override fun lookup(hostname: String): List<InetAddress> {
        cache[hostname]?.let { return it }
        if (enabled) {
            runCatching { googleDoh.lookup(hostname) }.getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { cache[hostname] = it; return it }
            runCatching { cloudflareDoh.lookup(hostname) }.getOrNull()
                ?.takeIf { it.isNotEmpty() }
                ?.let { cache[hostname] = it; return it }
        }
        return Dns.SYSTEM.lookup(hostname)
    }
}

object Http {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** Браузероподобный UA — многие CDN отдают ошибку неизвестным клиентам. */
    const val UA = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36 EvaStore/1.0"

    /** Включение/выключение DoH из настроек. */
    fun setDohEnabled(enabled: Boolean) {
        SmartDns.enabled = enabled
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .dns(SmartDns)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        // Браузероподобный UA для всех запросов без явного заголовка
        // (в том числе для загрузки иконок через Coil): многие CDN
        // отдают ошибку или пустой ответ неизвестным клиентам.
        .addInterceptor { chain ->
            val original = chain.request()
            val request = if (original.header("User-Agent") == null)
                original.newBuilder().header("User-Agent", UA).build()
            else original
            chain.proceed(request)
        }
        .build()

    /**
     * Прогрев DNS-кэша в фоне: заранее резолвим хосты маркетов и CDN
     * картинок, чтобы первый поиск и иконки не ждали медленные DoH-запросы.
     */
    fun prewarmDns() {
        val hosts = listOf(
            "tapi.pureapk.com", "d.apkpure.com", "image.winudf.com",
            "backapi.rustore.ru", "static.rustore.ru", "ws75.aptoide.com",
            "search.f-droid.org", "f-droid.org", "api.github.com"
        )
        Thread {
            hosts.forEach { host -> runCatching { SmartDns.lookup(host) } }
        }.apply { isDaemon = true; name = "dns-prewarm" }.start()
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): String =
        withContext(Dispatchers.IO) {
            val builder = Request.Builder().url(url)
                .header("User-Agent", UA)
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
                .header("User-Agent", UA)
                .head()
                .build()
            client.newCall(request).await().use { resp ->
                when {
                    resp.isSuccessful -> true
                    // Некоторые серверы не любят HEAD — пробуем GET.
                    resp.code == 405 || resp.code == 403 -> {
                        val getReq = Request.Builder().url(url)
                            .header("User-Agent", UA)
                            .build()
                        client.newCall(getReq).await().use { it.isSuccessful }
                    }
                    else -> false
                }
            }
        }.getOrDefault(false)
    }

    suspend fun postJson(url: String, jsonBody: String): String =
        withContext(Dispatchers.IO) {
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url)
                .header("User-Agent", UA)
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
