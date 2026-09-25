package com.abugdn.wid.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TranslationGlossaryTest {
    private fun pre(s: String) = TranslationGlossary.preprocess(s)
    private fun post(s: String) = TranslationGlossary.postprocess(s)

    @Test fun acronymsBecomeFullNames() {
        assertEquals("United States troops arrive in Iraq", pre("US troops arrive in Iraq"))
        assertEquals("United States-led coalition", pre("U.S.-led coalition"))
        assertEquals("Israel Defense Forces says", pre("IDF says"))
        assertEquals("United Nations and European Union", pre("UN and EU"))
        assertEquals("Islamic Revolutionary Guard Corps commander", pre("IRGC commander"))
        assertEquals("Islamic State fighters", pre("ISIS fighters"))
    }

    @Test fun pronounUsIsKept() {
        assertEquals("They told us to leave", pre("They told us to leave"))
        assertEquals("Join us", pre("Join us"))
    }

    @Test fun prIsNotATime() {
        assertEquals("Israeli Prime Minister Netanyahu", pre("Israeli PM Netanyahu"))
        assertEquals("Sirens at 5 PM", pre("Sirens at 5 PM"))
    }

    @Test fun ambiguousMilitaryTerms() {
        assertEquals("Israel launches air attacks on Beirut", pre("Israel launches airstrikes on Beirut"))
        assertEquals("United States attacks Houthi targets", pre("US strikes Houthi targets"))
        assertEquals("Prisoners on hunger strike", pre("Prisoners on hunger strike"))
        assertEquals("Heavy artillery fire in Kharkiv", pre("Heavy shelling in Kharkiv"))
        assertEquals("a volley of rockets", pre("a barrage of rockets"))
        assertEquals("United States sends aircraft carrier", pre("US sends carrier"))
        assertEquals("aircraft carrier group", pre("aircraft carrier group"))
        assertEquals("air raid sirens", pre("air raid sirens"))
        assertEquals("Attack on Gaza", pre("Strike on Gaza"))
    }

    @Test fun ambiguousCountries() {
        assertEquals("Türkiye's army", pre("Turkey's army"))
        assertEquals("Exército da Turquia", post("Exército da Türkiye"))
        assertEquals("Cisjordânia", pre("West Bank"))
    }

    @Test fun portugalToBrazil() {
        assertEquals("Irã e Iêmen no Oriente Médio", post("Irão e Iémen no Médio Oriente"))
        assertEquals("soldados israelenses", post("soldados israelitas"))
        assertEquals("civis palestinos", post("civis palestinianos"))
        assertEquals("Moscou e Teerã", post("Moscovo e Teerão"))
        assertEquals("O exército está atacando", post("O exército está a atacar"))
        assertEquals("Estão combatendo", post("Estão a combater"))
    }

    @Test fun militaryFixes() {
        assertEquals("Ataques aéreos em Gaza", post("Greves aéreas em Gaza"))
        assertEquals("ataque contra Beirute", post("greve contra Beirute"))
        assertEquals("presos em greve de fome", post("presos em greve de fome"))
        assertEquals("projéteis de artilharia", post("conchas de artilharia"))
        assertEquals("saraivada de foguetes", post("barragem de foguetes"))
        assertEquals("EUA enviam porta-aviões", post("EUA enviam transportadora de aeronaves"))
        assertEquals("Forças de Defesa de Israel", post("Força de Defesa de Israel"))
        assertEquals("Forças de Defesa de Israel", post("Forças de Defesa Israelenses"))
        assertEquals("OTAN", post("NATO"))
        assertEquals("Hezbollah", post("Hezbolá"))
    }

    @Test fun leftoverEnglishNames() {
        assertEquals("Líbano e Síria", post("Lebanon e Syria"))
        assertEquals("Mar Vermelho", post("Red Sea"))
        assertEquals("vale do rio Jordão", post("vale do Jordan River"))
        assertEquals("ataque em Beirute", post("ataque em Beirut"))
    }

    @Test fun unitedStatesAgreement() {
        assertEquals("Os Estados Unidos", post("O Estados Unidos"))
        assertEquals("tropas dos Estados Unidos", post("tropas do Estados Unidos"))
        assertEquals("nos Estados Unidos", post("no Estados Unidos"))
        assertEquals("coalizão liderada pelos Estados Unidos", post("coalizão liderada por Estados Unidos"))
        assertEquals("aos Estados Unidos", post("ao Estados Unidos"))
    }

    @Test fun fullRoundTripShape() {
        // O tradutor fica no meio; aqui simulamos a saída típica do ML Kit.
        val en = pre("US strikes Iran-backed militia in Iraq after IDF shelling")
        assertEquals("United States attacks Iran-backed militia in Iraq after Israel Defense Forces artillery fire", en)
        assertEquals(
            "Os Estados Unidos atacam milícia apoiada pelo Irã no Iraque após fogo de artilharia das Forças de Defesa de Israel",
            post("O Estados Unidos atacam milícia apoiada pelo Irão no Iraque após fogo de artilharia das Força de Defesa de Israel"),
        )
    }
}
