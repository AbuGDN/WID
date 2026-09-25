package com.abugdn.wid.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Boletim semanal do Argos: o resumo dos últimos 7 dias em uma imagem. */
data class Bulletin(
    val from: LocalDate,
    val to: LocalDate,
    val top: List<Cluster>,
    /** Região mais tensa: rótulo e índice (0–100). */
    val tenseRegion: Pair<String, Int>?,
    /** Maior pico de alerta da semana (alta incomum de maior ritmo ou tensão mais alta). */
    val biggestAlert: VigilEvent?,
    val alerts: Int,
    val first: FirstRank?,
)

fun buildBulletin(
    days: List<HistoryDay>,
    feed: Feed?,
    vigil: List<VigilEvent>,
    first: FirstStats?,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault(),
): Bulletin {
    val from = today.minusDays(6)
    val week = vigil.filter { !Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate().isBefore(from) }

    // Tensão: pico registrado na semana; sem registro, o índice atual mais alto.
    val peakTension = week.filter { it.kind == "tension" && it.region != null }.maxByOrNull { it.value }
    val tense = peakTension?.let { (TAG_LABELS[it.region] ?: it.region!!) to it.value.toInt() }
        ?: feed?.regions?.maxByOrNull { it.value.tension }
            ?.takeIf { it.value.tension > 0 }
            ?.let { (TAG_LABELS[it.key] ?: it.key) to it.value.tension }

    val biggest = week.filter { it.kind == "spike" }.maxByOrNull { it.value }
        ?: peakTension
        ?: week.firstOrNull { it.kind == "urgent" }

    return Bulletin(
        from = from,
        to = today,
        top = weekTop(days.filter { runCatching { !LocalDate.parse(it.date).isBefore(from) }.getOrDefault(false) }).map { it.top },
        tenseRegion = tense,
        biggestAlert = biggest,
        alerts = week.size,
        first = first?.ranking?.firstOrNull { it.firsts > 0 },
    )
}
