package com.abugdn.wid.data

/**
 * Alcance aproximado dos principais arsenais e defesas, de fontes abertas (até 2025).
 * As posições das defesas são ilustrativas: a localização real das baterias não é pública.
 */
data class RangeArc(
    val id: String,
    val label: String,
    val detail: String,
    val lat: Double,
    val lon: Double,
    val km: Int,
    val defense: Boolean,
)

val RANGES = listOf(
    RangeArc(
        "iran",
        "Irã · mísseis balísticos de médio alcance",
        "Shahab-3, Emad, Sejjil e Khorramshahr: cerca de 2.000 km a partir do oeste do Irã. Cobrem Israel inteiro, o Golfo e a Turquia.",
        34.3, 47.1, 2000, defense = false,
    ),
    RangeArc(
        "houthis",
        "Houthis (Iêmen) · mísseis de longo alcance",
        "Burkan-3 e “Palestina-2”, derivados de modelos iranianos: cerca de 2.000 km a partir de Sanaa, o bastante para chegar a Eilat e Tel Aviv.",
        15.4, 44.2, 2000, defense = false,
    ),
    RangeArc(
        "hezbollah",
        "Hezbollah (Líbano) · mísseis de precisão",
        "Fateh-110 / M-600: cerca de 300 km a partir do sul do Líbano; alcançam praticamente todo o território de Israel.",
        33.3, 35.5, 300, defense = false,
    ),
    RangeArc(
        "hamas",
        "Hamas (Gaza) · foguetes de longo alcance",
        "M-75 e R-160: até ~160 km. A maior parte do arsenal são foguetes de 20 a 40 km.",
        31.45, 34.4, 160, defense = false,
    ),
    RangeArc(
        "atacms",
        "Ucrânia · ATACMS (EUA)",
        "Míssil tático americano: até ~300 km a partir da linha de frente; usado contra a Crimeia e bases no sul da Rússia.",
        47.6, 35.3, 300, defense = false,
    ),
    RangeArc(
        "iskander",
        "Rússia · Iskander-M",
        "Míssil balístico de curto alcance: ~500 km a partir da fronteira (aqui, Belgorod).",
        50.6, 36.6, 500, defense = false,
    ),
    RangeArc(
        "iron_dome",
        "Israel · Domo de Ferro (uma bateria)",
        "Intercepta foguetes e morteiros a até ~70 km por bateria; Israel opera cerca de dez. Posição ilustrativa (Ashkelon).",
        31.67, 34.57, 70, defense = true,
    ),
    RangeArc(
        "thaad",
        "THAAD (EUA) em Israel",
        "Bateria americana enviada em outubro de 2024: intercepta mísseis balísticos na fase final, a ~200 km. Posição ilustrativa (Negev).",
        31.1, 35.0, 200, defense = true,
    ),
)
