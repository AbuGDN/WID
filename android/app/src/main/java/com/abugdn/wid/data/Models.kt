package com.abugdn.wid.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Espelha o feed.json gerado por backend/wid/build.py. */
@Serializable
data class Feed(
    val version: Int = 1,
    @SerialName("generated_at") val generatedAt: String = "",
    @SerialName("top_of_day") val topOfDay: Cluster? = null,
    val clusters: List<Cluster> = emptyList(),
)

@Serializable
data class Cluster(
    val id: String,
    val title: String,
    val summary: String = "",
    val url: String,
    val source: String,
    val lang: String = "en",
    val image: String? = null,
    val published: String,
    val updated: String = published,
    val tags: List<String> = emptyList(),
    @SerialName("sources_count") val sourcesCount: Int = 1,
    val score: Double = 0.0,
    @SerialName("day_score") val dayScore: Double = 0.0,
    val urgent: Boolean = false,
    val articles: List<ArticleRef> = emptyList(),
)

@Serializable
data class ArticleRef(
    val id: String,
    val title: String,
    val summary: String = "",
    val url: String,
    val source: String,
    val lang: String = "en",
    val published: String,
    val image: String? = null,
)

/** Texto completo baixado e guardado no aparelho. */
@Serializable
data class FullText(
    val url: String,
    val source: String,
    val lang: String,
    val paragraphs: List<String>,
    val translated: List<String>? = null,
    val fetchedAt: Long = System.currentTimeMillis(),
)

val json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

val TAG_LABELS = linkedMapOf(
    "israel" to "Israel",
    "gaza" to "Gaza",
    "cisjordania" to "Cisjordânia",
    "libano" to "Líbano",
    "ira" to "Irã",
    "iemen" to "Iêmen",
    "siria" to "Síria",
    "iraque" to "Iraque",
    "ucrania_russia" to "Ucrânia/Rússia",
    "sudao" to "Sudão",
    "asia" to "Ásia",
    "africa" to "África",
    "otan" to "OTAN",
)
