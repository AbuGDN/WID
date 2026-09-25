package com.abugdn.wid.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuotesTest {
    @Test
    fun attributesQuoteWithSpeechVerbAndOnePerson() {
        val q = Quotes.extract("Netanyahu diz que Israel vai “responder com força total” ao ataque")
        assertEquals(listOf("netanyahu" to "responder com força total"), q)
    }

    @Test
    fun ignoresQuotesWithoutSpeechVerbOrWithTwoPeople() {
        assertTrue(Quotes.extract("Netanyahu e o “plano de paz” em debate").isEmpty())
        assertTrue(Quotes.extract("Trump disse que Putin quer “acabar com a guerra logo”").isEmpty())
    }

    @Test
    fun mergeSkipsRepeatedQuote() {
        val a = QuoteEntry("trump", "Vamos acabar com isso", "CNN", "c1", 1)
        val b = QuoteEntry("trump", "vamos acabar com isso", "NPR", "c2", 2)
        val log = Quotes.merge(emptyList(), listOf(a))
        assertEquals(1, Quotes.merge(log, listOf(b)).size)
    }
}

class CitiesAndVigilTest {
    private fun cluster(title: String, violation: Boolean = false, tags: List<String> = emptyList()) = Cluster(
        id = title.hashCode().toString(), title = title, url = "u", source = "s",
        published = "2026-09-25T10:00:00Z", truceViolation = violation, tags = tags,
    )

    @Test
    fun findsCitiesInSeveralLanguages() {
        val c = cluster("Explosões em Khan Younis e ataque perto de Isfahan")
        assertEquals(setOf("Khan Younis", "Isfahan"), c.cities { it }.map { it.name }.toSet())
        assertEquals(listOf("Beirute"), cluster("غارات على بيروت").cities { it }.map { it.name })
        assertTrue(cluster("Gaza ceasefire talks").cities { it }.isEmpty())
    }

    @Test
    fun logsTruceViolationsAndHighClock() {
        val feed = Feed(
            global = GlobalClock(index = 78, level = "crítica", leader = "gaza"),
            clusters = listOf(cluster("Hamas violated ceasefire", violation = true, tags = listOf("israel", "gaza"))),
        )
        val kinds = Vigil.detect(feed, 1L, "2026-09-25").associateBy { it.kind }
        assertEquals("gaza", kinds.getValue("truce").region)
        assertEquals("clock:crítica:2026-09-25", kinds.getValue("clock").key)
    }
}
