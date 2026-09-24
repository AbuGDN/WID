package com.abugdn.wid.data

import android.content.Context
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.dankito.readability4j.extended.Readability4JExtended
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

private const val DATA_URL = "https://raw.githubusercontent.com/AbuGDN/WID/gh-pages"
const val FEED_URL = "$DATA_URL/feed.json"

private const val USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0 Mobile Safari/537.36"

class Repository(context: Context) {
    val storage = Storage(context)
    val translator = Translator(storage)
    val settings = SettingsStore(storage.prefs)
    val updater = Updater(context.applicationContext)

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _feed = MutableStateFlow(storage.loadFeed())
    val feed: StateFlow<Feed?> = _feed.asStateFlow()

    private val _saved = MutableStateFlow(storage.loadSaved())
    val saved: StateFlow<List<Cluster>> = _saved.asStateFlow()

    private val _archive = MutableStateFlow<List<HistoryDay>?>(null)
    val archive: StateFlow<List<HistoryDay>?> = _archive.asStateFlow()

    /** Baixa o feed, traduz títulos/resumos em inglês e só então publica para a UI. */
    suspend fun refresh(): Result<Feed> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = get("$FEED_URL?t=${System.currentTimeMillis() / 60_000}")
            val feed = json.decodeFromString<Feed>(raw)
            val texts = feedTexts(feed) + textsOf(_saved.value) + textsOf(_archive.value.orEmpty().map { it.top })
            translator.translateAll(texts)
            storage.saveTranslations(keep = texts)
            storage.saveFeed(raw)
            _feed.value = feed
            feed
        }
    }

    private fun feedTexts(feed: Feed): Set<String> = textsOf(feed.clusters)

    private fun textsOf(clusters: List<Cluster>): Set<String> = buildSet {
        for (c in clusters) {
            if (c.lang != "pt") {
                add(c.title)
                if (c.summary.isNotBlank()) add(c.summary)
            }
            c.articles.filter { it.lang != "pt" }.forEach { add(it.title) }
        }
    }

    /** Procura no feed atual, depois nas salvas e no arquivo. */
    fun cluster(id: String): Cluster? =
        _feed.value?.clusters?.firstOrNull { it.id == id }
            ?: _saved.value.firstOrNull { it.id == id }
            ?: _archive.value?.firstOrNull { it.top.id == id }?.top

    fun isSaved(id: String) = _saved.value.any { it.id == id }

    /** Salva (sem prazo de validade) ou remove dos salvos. */
    fun toggleSaved(cluster: Cluster) {
        val list = _saved.value
        val next = if (list.any { it.id == cluster.id }) list.filter { it.id != cluster.id } else listOf(cluster) + list
        storage.saveSaved(next)
        _saved.value = next
    }

    /** Principal de cada um dos últimos [days] dias (history/ no servidor). */
    suspend fun loadArchive(days: Int = 60): Result<List<HistoryDay>> = withContext(Dispatchers.IO) {
        runCatching {
            val index = json.decodeFromString<HistoryIndex>(get("$DATA_URL/history/index.json?t=${System.currentTimeMillis() / 600_000}"))
            val list = coroutineScope {
                index.days.take(days).map { day ->
                    async { runCatching { json.decodeFromString<HistoryDay>(get("$DATA_URL/history/$day.json")) }.getOrNull() }
                }.awaitAll().filterNotNull()
            }
            translator.translateAll(textsOf(list.map { it.top }))
            _archive.value = list
            list
        }
    }

    /**
     * Texto completo da história. Prefere um veículo em português (não precisa
     * traduzir); senão baixa o principal e traduz parágrafo por parágrafo.
     */
    suspend fun fullText(cluster: Cluster, translate: Boolean = true): Result<FullText> =
        withContext(Dispatchers.IO) {
            storage.loadFullText(cluster.id)?.let { cached ->
                if (cached.lang == "pt" || cached.translated != null || !translate) {
                    return@withContext Result.success(cached)
                }
                return@withContext Result.success(addTranslation(cluster.id, cached))
            }
            val candidates = buildList {
                addAll(cluster.articles.filter { it.lang == "pt" }.map { Triple(it.url, it.source, it.lang) })
                add(Triple(cluster.url, cluster.source, cluster.lang))
                addAll(cluster.articles.filter { it.lang != "pt" }.map { Triple(it.url, it.source, it.lang) })
            }.distinctBy { it.first }

            var lastError: Throwable = IOException("sem artigos")
            for ((url, source, lang) in candidates.take(4)) {
                val result = runCatching { extract(url) }
                val paragraphs = result.getOrNull()
                if (paragraphs != null && paragraphs.joinToString("").length >= 400) {
                    var text = FullText(url = url, source = source, lang = lang, paragraphs = paragraphs)
                    storage.saveFullText(cluster.id, text)
                    if (lang != "pt" && translate) text = addTranslation(cluster.id, text)
                    return@withContext Result.success(text)
                }
                lastError = result.exceptionOrNull() ?: IOException("texto curto demais (paywall?)")
            }
            Result.failure(lastError)
        }

    private suspend fun addTranslation(id: String, text: FullText): FullText {
        val translated = translator.translateList(text.paragraphs) ?: return text
        return text.copy(translated = translated).also { storage.saveFullText(id, it) }
    }

    /** Baixa antecipadamente o texto das principais histórias, para ler offline. */
    suspend fun prefetch(feed: Feed, limit: Int = 10) {
        val top = listOfNotNull(feed.topOfDay) + feed.clusters.take(limit)
        for (c in top.distinctBy { it.id }) fullText(c)
        storage.pruneFullTexts(keep = _saved.value.map { it.id }.toSet())
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw IOException("resposta vazia")
        }
    }

    private fun extract(url: String): List<String> {
        val html = get(url)
        val article = Readability4JExtended(url, html).parse()
        val content = article.content ?: return emptyList()
        return Jsoup.parse(content).select("p, li")
            .map { it.text().trim() }
            .filter { it.length >= 40 }
            .distinct()
    }
}
