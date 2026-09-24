package com.abugdn.wid.data

import android.content.Context
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** Tudo fica em arquivos JSON na pasta privada do app: é pouco dado e só um usuário. */
class Storage(context: Context) {
    private val dir = context.filesDir
    private val feedFile = File(dir, "feed.json")
    private val translationsFile = File(dir, "translations.json")
    private val savedFile = File(dir, "saved.json")
    private val articlesDir = File(dir, "articles").apply { mkdirs() }
    val prefs = context.getSharedPreferences("wid", Context.MODE_PRIVATE)

    /** Tradução de títulos e resumos do feed (original -> português). */
    val translations: MutableMap<String, String> = ConcurrentHashMap(loadTranslations())

    fun loadFeed(): Feed? = runCatching {
        json.decodeFromString<Feed>(feedFile.readText())
    }.getOrNull()

    fun saveFeed(raw: String) = writeAtomic(feedFile, raw)

    private fun loadTranslations(): Map<String, String> = runCatching {
        json.decodeFromString(MapSerializer(String.serializer(), String.serializer()), translationsFile.readText())
    }.getOrDefault(emptyMap())

    @Synchronized
    fun saveTranslations(keep: Set<String>? = null) {
        if (keep != null) translations.keys.retainAll(keep)
        val snapshot = HashMap(translations)
        writeAtomic(translationsFile, json.encodeToString(MapSerializer(String.serializer(), String.serializer()), snapshot))
    }

    fun loadFullText(id: String): FullText? = runCatching {
        json.decodeFromString<FullText>(File(articlesDir, "$id.json").readText())
    }.getOrNull()

    fun saveFullText(id: String, text: FullText) =
        writeAtomic(File(articlesDir, "$id.json"), json.encodeToString(FullText.serializer(), text))

    fun loadSaved(): List<Cluster> = runCatching {
        json.decodeFromString(ListSerializer(Cluster.serializer()), savedFile.readText())
    }.getOrDefault(emptyList())

    fun saveSaved(list: List<Cluster>) =
        writeAtomic(savedFile, json.encodeToString(ListSerializer(Cluster.serializer()), list))

    /** Apaga textos completos com mais de [maxAgeDays] dias, menos os das notícias salvas. */
    fun pruneFullTexts(keep: Set<String>, maxAgeDays: Int = 30) {
        val limit = System.currentTimeMillis() - maxAgeDays * 86_400_000L
        articlesDir.listFiles()
            ?.filter { it.lastModified() < limit && it.nameWithoutExtension !in keep }
            ?.forEach { it.delete() }
    }

    private fun writeAtomic(file: File, content: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(content)
        tmp.renameTo(file)
    }
}
