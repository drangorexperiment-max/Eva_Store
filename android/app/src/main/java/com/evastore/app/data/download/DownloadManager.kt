package com.evastore.app.data.download

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.evastore.app.data.network.Http
import com.evastore.app.data.network.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.Request
import java.io.File
import java.io.IOException

enum class DownloadStatus { QUEUED, DOWNLOADING, SCANNING, DONE, FAILED }

data class DownloadTask(
    val id: String,
    val appName: String,
    val fileName: String,
    val url: String,
    val iconUrl: String?,
    val marketLabel: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val totalBytes: Long = 0,
    val filePath: String? = null,
    val error: String? = null
)

/**
 * Загрузчик APK: качает во внутреннюю папку downloads/ с прогрессом,
 * после — предлагает установку через системный установщик пакетов.
 */
class ApkDownloader(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks

    private val downloadsDir: File
        get() = File(context.filesDir, "downloads").apply { mkdirs() }

    fun enqueue(
        id: String,
        appName: String,
        fileName: String,
        url: String,
        iconUrl: String?,
        marketLabel: String,
        onDownloaded: suspend (File) -> Unit = {}
    ) {
        if (_tasks.value.any { it.id == id && it.status == DownloadStatus.DOWNLOADING }) return

        val task = DownloadTask(id, appName, fileName, url, iconUrl, marketLabel)
        _tasks.update { list -> list.filterNot { it.id == id } + task }

        scope.launch {
            try {
                update(id) { it.copy(status = DownloadStatus.DOWNLOADING) }
                var file = downloadFile(id, url, fileName)
                // Некоторые маркеты (RuStore) отдают zip-контейнер с APK внутри.
                if (isZip(file)) {
                    file = extractApk(file, fileName)
                }
                update(id) {
                    it.copy(
                        status = DownloadStatus.DONE,
                        progress = 1f,
                        filePath = file.absolutePath
                    )
                }
                onDownloaded(file)
            } catch (e: Exception) {
                update(id) {
                    it.copy(status = DownloadStatus.FAILED, error = e.message ?: "Ошибка загрузки")
                }
            }
        }
    }

    fun setScanning(id: String, scanning: Boolean) {
        update(id) {
            it.copy(status = if (scanning) DownloadStatus.SCANNING else DownloadStatus.DONE)
        }
    }

    fun remove(id: String) {
        _tasks.value.firstOrNull { it.id == id }?.filePath?.let { path ->
            runCatching { File(path).delete() }
        }
        _tasks.update { list -> list.filterNot { it.id == id } }
    }

    private suspend fun downloadFile(id: String, url: String, fileName: String): File {
        val request = Request.Builder().url(url)
            .header("User-Agent", "EvaStore/1.0")
            .build()
        Http.client.newCall(request).await().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("Пустой ответ сервера")
            val total = body.contentLength()
            update(id) { it.copy(totalBytes = total) }

            val outFile = File(downloadsDir, fileName.sanitized())
            body.byteStream().use { input ->
                outFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    var lastEmit = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        // Троттлим обновления прогресса, чтобы UI не лагал
                        if (total > 0 && downloaded - lastEmit > total / 200) {
                            lastEmit = downloaded
                            val p = downloaded.toFloat() / total
                            update(id) { it.copy(progress = p) }
                        }
                    }
                }
            }
            return outFile
        }
    }

    /** APK — это тоже zip, поэтому смотрим содержимое, а не сигнатуру. */
    private fun isZip(file: File): Boolean =
        file.inputStream().use { input ->
            val magic = ByteArray(2)
            input.read(magic) == 2 && magic[0] == 'P'.code.toByte() && magic[1] == 'K'.code.toByte()
        }

    /**
     * Если файл — контейнер (внутри лежит *.apk, как у RuStore) — достаём
     * самый крупный APK. Если это уже сам APK (есть AndroidManifest.xml) —
     * возвращаем как есть.
     */
    private fun extractApk(file: File, desiredName: String): File {
        java.util.zip.ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence().toList()
            // Это уже готовый APK?
            if (entries.any { it.name == "AndroidManifest.xml" }) return file

            val apkEntry = entries
                .filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                .maxByOrNull { it.size }
                ?: return file // APK внутри нет — оставляем как есть

            val outName = desiredName.sanitized().removeSuffix(".zip")
                .let { if (it.endsWith(".apk")) it else "$it.apk" }
            val outFile = File(downloadsDir, outName)
            zip.getInputStream(apkEntry).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            file.delete()
            return outFile
        }
    }

    fun installApk(filePath: String) {
        val file = File(filePath)
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun update(id: String, transform: (DownloadTask) -> DownloadTask) {
        _tasks.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun String.sanitized(): String = replace(Regex("[^A-Za-z0-9._-]"), "_")
}
