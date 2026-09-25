package com.abugdn.wid.data

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Tradução inglês -> português no próprio aparelho (ML Kit). O modelo (~30 MB)
 * é baixado uma vez; depois funciona offline e sem limite.
 */
class Translator(private val storage: Storage) {
    private val client = Translation.getClient(
        TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.PORTUGUESE)
            .build()
    )
    private val mutex = Mutex()
    @Volatile private var modelReady = false

    /** Com a economia de dados ligada, o modelo (~30 MB) só baixa no Wi-Fi. */
    @Volatile var wifiOnly = false

    /** Texto traduzido se já estiver em cache; senão, o original. */
    fun cached(text: String): String = storage.translations[text] ?: text

    fun display(text: String, lang: String): String = if (lang == "pt") text else cached(text)

    private suspend fun ensureModel(): Boolean {
        if (modelReady) return true
        modelReady = runCatching {
            val conditions = DownloadConditions.Builder().apply { if (wifiOnly) requireWifi() }.build()
            client.downloadModelIfNeeded(conditions).await()
        }.isSuccess
        return modelReady
    }

    /** ML Kit com o glossário de guerra em volta (siglas, países, termos militares). */
    private suspend fun translateOne(text: String): String =
        TranslationGlossary.postprocess(client.translate(TranslationGlossary.preprocess(text)).await())

    /** Traduz e guarda no cache o que ainda não foi traduzido. */
    suspend fun translateAll(texts: Collection<String>): Boolean {
        val missing = texts.filter { it.isNotBlank() && it !in storage.translations }.distinct()
        if (missing.isEmpty()) return true
        if (!ensureModel()) return false
        mutex.withLock {
            for (text in missing) {
                runCatching { translateOne(text) }
                    .onSuccess { storage.translations[text] = it }
            }
        }
        return true
    }

    /** Traduz uma lista mantendo a ordem (usado no texto completo, não vai para o cache). */
    suspend fun translateList(texts: List<String>): List<String>? {
        if (!ensureModel()) return null
        return mutex.withLock {
            texts.map { runCatching { translateOne(it) }.getOrDefault(it) }
        }
    }
}
