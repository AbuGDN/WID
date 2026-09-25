package com.abugdn.wid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class VigilTest {
    private fun cluster(id: String, urgent: Boolean = false, figures: Map<String, FigureInfo> = emptyMap()) = Cluster(
        id = id, title = "t$id", url = "u", source = "s", published = "2026-09-25T10:00:00Z",
        urgent = urgent, figures = figures,
    )

    private val feed = Feed(
        regions = mapOf(
            "gaza" to RegionStat(tension = 82, level = "crítica", spike = true, spikeRatio = 3.4),
            "iemen" to RegionStat(tension = 30, level = "moderada"),
        ),
        clusters = listOf(
            cluster("a", urgent = true),
            cluster("b", figures = mapOf("killed" to FigureInfo(mapOf("X" to 12, "Y" to 20), divergent = true))),
            cluster("c"),
        ),
    )

    @Test
    fun detectsEveryKindOfAlert() {
        val events = Vigil.detect(feed, now = 1_000L, day = "2026-09-25")
        assertEquals(setOf("urgent:a", "figures:b", "spike:gaza:2026-09-25", "tension:gaza:2026-09-25"), events.map { it.key }.toSet())
        assertEquals("mortos: 12 a 20", events.first { it.kind == "figures" }.detail)
        assertEquals(3.4, events.first { it.kind == "spike" }.value, 0.001)
    }

    @Test
    fun mergeDoesNotRepeatAndKeepsNewestFirst() {
        val first = Vigil.merge(emptyList(), Vigil.detect(feed, 1_000L, "2026-09-25"))
        assertSame(first, Vigil.merge(first, Vigil.detect(feed, 2_000L, "2026-09-25")))
        val nextDay = Vigil.merge(first, Vigil.detect(feed, 2_000_000_000_000L, "2026-09-26"))
        assertEquals(first.size + 2, nextDay.size)
        assertTrue(nextDay.first().key.endsWith("2026-09-26"))
    }
}

class BulletinTest {
    private val zone = java.time.ZoneOffset.UTC
    private val today = java.time.LocalDate.of(2026, 9, 27)
    private fun at(day: Int) = java.time.LocalDate.of(2026, 9, day).atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000

    @Test
    fun picksWeekPeaksAndIgnoresOlderAlerts() {
        val vigil = listOf(
            VigilEvent("spike:gaza:1", at(25), "spike", "Alta incomum: Gaza", region = "gaza", value = 3.1),
            VigilEvent("spike:iemen:1", at(22), "spike", "Alta incomum: Iêmen", region = "iemen", value = 4.5),
            VigilEvent("spike:ira:old", at(10), "spike", "Alta incomum: Irã", region = "ira", value = 9.0),
            VigilEvent("tension:libano:1", at(24), "tension", "Tensão crítica: Líbano", region = "libano", value = 80.0),
        )
        val day = { d: String, score: Double ->
            HistoryDay(d, Cluster(id = d, title = d, url = "u", source = "s", published = "${d}T10:00:00Z", dayScore = score))
        }
        val days = listOf(day("2026-09-26", 5.0), day("2026-09-25", 9.0), day("2026-09-10", 99.0))
        val first = FirstStats(ranking = listOf(FirstRank("A", firsts = 0), FirstRank("B", firsts = 4)))
        val b = buildBulletin(days, null, vigil, first, today, zone)

        assertEquals(listOf("2026-09-25", "2026-09-26"), b.top.map { it.id })
        assertEquals(TAG_LABELS["libano"] to 80, b.tenseRegion)
        assertEquals("spike:iemen:1", b.biggestAlert?.key)
        assertEquals(3, b.alerts)
        assertEquals("B", b.first?.source)
    }
}
