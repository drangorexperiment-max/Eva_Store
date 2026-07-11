package com.evastore.app.data.scan

import com.evastore.app.data.model.ScanResult
import com.evastore.app.data.network.Http
import com.evastore.app.data.network.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * VirusTotal API v3: сначала проверяем файл по SHA-256 (мгновенно, если
 * файл уже известен VT), иначе загружаем и опрашиваем анализ.
 */
class VirusTotalClient(private val apiKey: String) {

    suspend fun scanFile(file: File, onPhase: (String) -> Unit = {}): ScanResult {
        require(apiKey.isNotBlank()) { "Не задан API-ключ VirusTotal (Настройки → Антивирус)" }

        onPhase("Проверка хэша файла...")
        val sha256 = file.sha256()
        lookupByHash(sha256)?.let { return it }

        onPhase("Загрузка файла в VirusTotal...")
        val analysisId = uploadFile(file)

        onPhase("Анализ файла антивирусами...")
        repeat(30) {
            delay(10_000)
            pollAnalysis(analysisId)?.let { return it }
            onPhase("Анализ выполняется... (${(it + 1) * 10} c)")
        }
        throw IOException("VirusTotal не завершил анализ. Попробуйте позже — файл уже в очереди.")
    }

    private suspend fun lookupByHash(sha256: String): ScanResult? = runCatching {
        val body = Http.get(
            "https://www.virustotal.com/api/v3/files/$sha256",
            headers = mapOf("x-apikey" to apiKey)
        )
        parseStats(body, "last_analysis_stats", sha256)
    }.getOrNull()

    private suspend fun uploadFile(file: File): String = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", file.name,
                file.asRequestBody("application/octet-stream".toMediaType())
            )
            .build()
        val request = Request.Builder()
            .url("https://www.virustotal.com/api/v3/files")
            .header("x-apikey", apiKey)
            .post(requestBody)
            .build()
        Http.client.newCall(request).await().use { resp ->
            if (!resp.isSuccessful) throw IOException("VirusTotal HTTP ${resp.code}")
            val json = Http.json.parseToJsonElement(resp.body!!.string()).jsonObject
            json["data"]!!.jsonObject["id"]!!.jsonPrimitive.content
        }
    }

    private suspend fun pollAnalysis(analysisId: String): ScanResult? {
        val body = Http.get(
            "https://www.virustotal.com/api/v3/analyses/$analysisId",
            headers = mapOf("x-apikey" to apiKey)
        )
        val root = Http.json.parseToJsonElement(body).jsonObject
        val attrs = root["data"]?.jsonObject?.get("attributes")?.jsonObject ?: return null
        val status = attrs["status"]?.jsonPrimitive?.content
        if (status != "completed") return null
        return parseStats(body, "stats", null)
    }

    private fun parseStats(body: String, statsKey: String, sha256: String?): ScanResult {
        val root = Http.json.parseToJsonElement(body).jsonObject
        val attrs = root["data"]!!.jsonObject["attributes"]!!.jsonObject
        val stats = attrs[statsKey]!!.jsonObject
        fun stat(name: String) = stats[name]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

        val malicious = stat("malicious")
        val suspicious = stat("suspicious")
        val harmless = stat("harmless")
        val undetected = stat("undetected")
        return ScanResult(
            malicious = malicious,
            suspicious = suspicious,
            harmless = harmless,
            undetected = undetected,
            total = malicious + suspicious + harmless + undetected,
            permalink = sha256?.let { "https://www.virustotal.com/gui/file/$it" }
        )
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
