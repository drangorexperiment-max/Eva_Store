package com.evastore.app.data.sources

import com.evastore.app.data.model.DownloadOption
import com.evastore.app.data.model.Market
import com.evastore.app.data.model.StoreApp
import com.evastore.app.data.network.Http
import kotlinx.serialization.Serializable

/**
 * GitHub: ищем репозитории Android-приложений и берём APK из последнего релиза.
 */
object GithubSource : MarketSource {

    override val market = Market.GITHUB

    @Serializable
    private data class RepoSearch(val items: List<Repo> = emptyList())

    @Serializable
    private data class Repo(
        val full_name: String = "",
        val name: String = "",
        val description: String? = null,
        val owner: Owner = Owner(),
        val stargazers_count: Int = 0
    )

    @Serializable
    private data class Owner(val login: String = "", val avatar_url: String? = null)

    @Serializable
    private data class Release(val tag_name: String = "", val assets: List<Asset> = emptyList())

    @Serializable
    private data class Asset(
        val name: String = "",
        val size: Long = 0,
        val browser_download_url: String = ""
    )

    override suspend fun search(query: String): List<StoreApp> {
        val q = "${query.urlEncode()}+android+topic:android"
        val body = Http.get(
            "https://api.github.com/search/repositories?q=$q&sort=stars&per_page=15",
            headers = mapOf("Accept" to "application/vnd.github+json")
        )
        val parsed = Http.json.decodeFromString<RepoSearch>(body)
        return parsed.items.map { repo ->
            StoreApp(
                id = "github:${repo.full_name}",
                name = repo.name,
                packageName = null,
                summary = repo.description.orEmpty(),
                iconUrl = repo.owner.avatar_url,
                developer = repo.owner.login,
                category = "Open Source",
                options = listOf(
                    DownloadOption(
                        market = Market.GITHUB,
                        url = "https://api.github.com/repos/${repo.full_name}/releases/latest"
                    )
                )
            )
        }
    }

    /** Возвращает (url, fileName, size) первого APK из последнего релиза или null. */
    suspend fun resolveApkUrl(repoFullNameApiUrl: String): Triple<String, String, Long>? {
        val body = Http.get(
            repoFullNameApiUrl,
            headers = mapOf("Accept" to "application/vnd.github+json")
        )
        val release = Http.json.decodeFromString<Release>(body)
        val apk = release.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
            ?: return null
        return Triple(apk.browser_download_url, apk.name, apk.size)
    }
}
