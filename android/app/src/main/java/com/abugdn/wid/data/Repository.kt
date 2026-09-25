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

    /** Feed como veio do servidor; [feed] é ele com os veículos escondidos/preferidos aplicados. */
    private var raw: Feed? = storage.loadFeed()
    private val _feed = MutableStateFlow(applySourcePrefs(raw, settings.value))
    val feed: StateFlow<Feed?> = _feed.asStateFlow()

    init {
        settings.onChange = { _feed.value = applySourcePrefs(raw, it) }
    }

    private val _followed = MutableStateFlow(loadFollowed())
    /** id -> quantos veículos a história tinha na última vez que avisamos. */
    val followed: StateFlow<Map<String, Int>> = _followed.asStateFlow()

    private val _stats = MutableStateFlow<DailyStats?>(null)
    val stats: StateFlow<DailyStats?> = _stats.asStateFlow()

    private val _saved = MutableStateFlow(storage.loadSaved())
    val saved: StateFlow<List<Cluster>> = _saved.asStateFlow()

    private val _read = MutableStateFlow(storage.prefs.getStringSet("read_ids", emptySet())!!.toSet())
    val read: StateFlow<Set<String>> = _read.asStateFlow()

    /** Horário da visita anterior: histórias que começaram depois disso são "novas". */
    var previousVisit: Long = storage.prefs.getLong("last_visit", 0)
        private set

    fun markRead(id: String) {
        if (id in _read.value) return
        val keep = (_feed.value?.clusters.orEmpty() + _saved.value).map { it.id }.toSet()
        val next = (_read.value + id).filter { it in keep || it == id }.toSet()
        storage.prefs.edit().putStringSet("read_ids", next).apply()
        _read.value = next
    }

    /** Chamado quando o app vai para o fundo: a próxima abertura conta novidades a partir daqui. */
    fun endVisit() {
        storage.prefs.edit().putLong("last_visit", System.currentTimeMillis()).apply()
    }

    fun startVisit() {
        previousVisit = storage.prefs.getLong("last_visit", 0)
    }

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
            this@Repository.raw = feed
            applySourcePrefs(feed, settings.value).also { _feed.value = it }!!
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

    /** Todos os veículos que aparecem no feed (para a tela de escolher fontes). */
    fun knownSources(): List<String> =
        (raw?.clusters.orEmpty().flatMap { c -> c.articles.map { it.source } + c.source } + settings.value.hiddenSources)
            .distinct().sorted()

    private fun loadFollowed(): Map<String, Int> =
        storage.prefs.getStringSet("followed", emptySet())!!.mapNotNull { entry ->
            val (id, count) = entry.split('|').takeIf { it.size == 2 } ?: return@mapNotNull null
            id to (count.toIntOrNull() ?: 0)
        }.toMap()

    private fun saveFollowed(map: Map<String, Int>) {
        storage.prefs.edit().putStringSet("followed", map.map { (id, n) -> "$id|$n" }.toSet()).apply()
        _followed.value = map
    }

    fun toggleFollow(cluster: Cluster) {
        val map = _followed.value
        saveFollowed(if (cluster.id in map) map - cluster.id else map + (cluster.id to cluster.sourcesCount))
    }

    /**
     * Histórias seguidas que ganharam veículos desde o último aviso: devolve (história, quantos novos).
     * Histórias que saíram do feed (mais de 48 h) deixam de ser seguidas.
     */
    fun followUpdates(feed: Feed): List<Pair<Cluster, Int>> {
        val map = _followed.value
        if (map.isEmpty()) return emptyList()
        val byId = feed.clusters.associateBy { it.id }
        val updates = map.mapNotNull { (id, count) ->
            byId[id]?.takeIf { it.sourcesCount > count }?.let { it to it.sourcesCount - count }
        }
        saveFollowed(map.filterKeys { it in byId }.mapValues { (id, n) -> byId[id]?.sourcesCount ?: n })
        return updates
    }

    suspend fun loadStats(): Result<DailyStats> = withContext(Dispatchers.IO) {
        runCatching {
            json.decodeFromString<DailyStats>(get("$DATA_URL/stats/daily.json?t=${System.currentTimeMillis() / 600_000}"))
                .also { _stats.value = it }
        }
    }

    fun isSaved(id: String) = _saved.value.any { it.id == id }

    /** Salva (sem prazo de validade) ou remove dos salvos. */
    fun toggleSaved(cluster: Cluster) {
        val list = _saved.value
        val next = if (list.any { it.id == cluster.id }) list.filter { it.id != cluster.id } else listOf(cluster) + list
        storage.saveSaved(next)
        _saved.value = next
    }

    /** Principal de cada um dos últimos [days] dias (history/ no servidor). */
    suspend fun loadArchive(days: Int = 60, publish: Boolean = true): Result<List<HistoryDay>> = withContext(Dispatchers.IO) {
        runCatching {
            val index = json.decodeFromString<HistoryIndex>(get("$DATA_URL/history/index.json?t=${System.currentTimeMillis() / 600_000}"))
            val list = coroutineScope {
                index.days.take(days).map { day ->
                    async { runCatching { json.decodeFromString<HistoryDay>(get("$DATA_URL/history/$day.json")) }.getOrNull() }
                }.awaitAll().filterNotNull()
            }
            translator.translateAll(textsOf(list.map { it.top }))
            if (publish) _archive.value = list
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

/**
 * Aplica os veículos escondidos (somem do grupo; o grupo some se ficar vazio) e os
 * preferidos (dão o título do grupo e sobem 30% no ranking).
 */
fun applySourcePrefs(feed: Feed?, s: Settings): Feed? {
    if (feed == null || (s.hiddenSources.isEmpty() && s.preferredSources.isEmpty())) return feed

    fun adjust(c: Cluster): Cluster? {
        val articles = c.articles.filter { it.source !in s.hiddenSources }
        if (articles.isEmpty() && c.articles.isNotEmpty()) return null
        if (c.articles.isEmpty() && c.source in s.hiddenSources) return null
        val sources = articles.map { it.source }.toSet().ifEmpty { setOf(c.source) }
        val preferred = articles.firstOrNull { it.source in s.preferredSources }
        val lead = when {
            preferred != null && c.source !in s.preferredSources -> preferred
            c.source in s.hiddenSources -> articles.firstOrNull { it.lang == "pt" } ?: articles.first()
            else -> null
        }
        val base = if (lead == null) c else c.copy(
            title = lead.title, summary = lead.summary, url = lead.url, source = lead.source,
            lang = lead.lang, image = lead.image ?: c.image,
        )
        val boost = if (sources.any { it in s.preferredSources }) 1.3 else 1.0
        return base.copy(
            articles = articles, sourcesCount = sources.size,
            score = c.score * boost, dayScore = c.dayScore * boost,
        )
    }

    val clusters = feed.clusters.mapNotNull(::adjust).sortedByDescending { it.score }
    val top = feed.topOfDay?.let { t -> clusters.firstOrNull { it.id == t.id } }
        ?: clusters.maxByOrNull { it.dayScore }
    return feed.copy(clusters = clusters, topOfDay = top)
}
