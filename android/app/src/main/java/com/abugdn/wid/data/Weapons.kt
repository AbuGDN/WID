package com.abugdn.wid.data

/** Ficha técnica resumida de uma arma do glossário (fontes abertas, até 2025). */
data class WeaponSheet(
    val origin: String,
    val type: String,
    val range: String,
    val usedIn: String,
    /** Círculo de alcance correspondente no mapa (RANGES), se houver. */
    val rangeId: String? = null,
)

val WEAPON_SHEETS = mapOf(
    "domo" to WeaponSheet("Israel (Rafael), com financiamento dos EUA", "Defesa contra foguetes, morteiros e drones", "4 a 70 km por bateria", "Gaza, Líbano, Iêmen, Irã (desde 2011)", "iron_dome"),
    "funda" to WeaponSheet("Israel e EUA (Rafael e Raytheon)", "Defesa aérea de médio alcance", "40 a 300 km", "Gaza, Líbano, Irã (desde 2017)"),
    "arrow" to WeaponSheet("Israel e EUA (IAI e Boeing)", "Defesa contra mísseis balísticos, inclusive fora da atmosfera", "Arrow 3: intercepta a mais de 100 km de altitude", "Houthis e Irã (2023–2025)"),
    "thaad" to WeaponSheet("EUA (Lockheed Martin)", "Defesa contra mísseis balísticos na fase final", "~200 km", "Israel (2024–2025), Emirados, Arábia Saudita", "thaad"),
    "patriot" to WeaponSheet("EUA (Raytheon)", "Defesa aérea e antimíssil", "até ~160 km contra aviões; menos contra balísticos", "Ucrânia, Israel, Golfo, Europa"),
    "shahed" to WeaponSheet("Irã; produzido na Rússia como Geran-2", "Drone de ataque (“kamikaze”)", "até ~2.000 km", "Ucrânia (pela Rússia), Israel, navios no Mar Vermelho"),
    "himars" to WeaponSheet("EUA (Lockheed Martin)", "Lançador de foguetes sobre rodas", "~80 km (GMLRS) ou 300 km (ATACMS)", "Ucrânia (desde 2022)"),
    "atacms" to WeaponSheet("EUA (Lockheed Martin)", "Míssil balístico tático", "até ~300 km", "Ucrânia (desde 2023)", "atacms"),
    "storm" to WeaponSheet("Reino Unido e França (MBDA)", "Míssil de cruzeiro lançado de avião", "mais de 250 km", "Ucrânia (desde 2023)"),
    "kinzhal" to WeaponSheet("Rússia", "Míssil balístico lançado de avião (divulgado como hipersônico)", "~2.000 km, segundo a Rússia", "Ucrânia"),
    "tomahawk" to WeaponSheet("EUA (Raytheon)", "Míssil de cruzeiro lançado de navios e submarinos", "mais de 1.500 km", "Iraque, Síria, Iêmen, Irã"),
    "gbu57" to WeaponSheet("EUA (Boeing)", "Bomba antibunker de 13,6 t (só o B-2 carrega)", "lançada do avião sobre o alvo", "Irã, Fordow (junho de 2025)"),
    "f35" to WeaponSheet("EUA (Lockheed Martin)", "Caça furtivo de 5ª geração", "raio de combate ~1.000 km", "Israel: Síria, Líbano, Irã, Iêmen"),
    "ira_balisticos" to WeaponSheet("Irã", "Mísseis balísticos de médio alcance", "~2.000 km", "Israel (2024–2025)", "iran"),
    "fattah" to WeaponSheet("Irã", "Míssil balístico (chamado pelo Irã de hipersônico)", "~1.400 km", "Israel (2024–2025, segundo o Irã)", "iran"),
    "kheibar" to WeaponSheet("Irã", "Míssil balístico de combustível sólido", "~1.450 km", "Israel (2024–2025)", "iran"),
    "fateh110" to WeaponSheet("Irã; versões na Síria e com o Hezbollah (M-600)", "Míssil balístico de curto alcance", "~300 km", "Síria, Iraque, Líbano", "hezbollah"),
    "burkan" to WeaponSheet("Houthis, derivado do iraniano Qiam", "Míssil balístico", "até ~2.000 km nas versões recentes", "Arábia Saudita, Israel", "houthis"),
    "iskander" to WeaponSheet("Rússia", "Míssil balístico de curto alcance", "~500 km", "Ucrânia, Geórgia (2008)", "iskander"),
    "oreshnik" to WeaponSheet("Rússia", "Míssil balístico de alcance intermediário, várias ogivas", "mais de 3.000 km (estimado)", "Ucrânia, Dnipro (novembro de 2024)"),
    "lancet" to WeaponSheet("Rússia (ZALA)", "Drone de ataque (munição vagante)", "~40 a 70 km", "Ucrânia"),
    "bayraktar" to WeaponSheet("Turquia (Baykar)", "Drone armado de média altitude", "~150 km do controle", "Nagorno-Karabakh, Líbia, Síria, Ucrânia"),
    "kornet" to WeaponSheet("Rússia", "Míssil antitanque guiado", "5 a 10 km", "Hezbollah e Hamas contra Israel; Síria, Iraque"),
)
