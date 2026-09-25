package com.abugdn.wid.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Contador "dia N" dos conflitos com início bem definido. */
data class Conflict(val label: String, val start: LocalDate) {
    fun day(today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(start, today) + 1
}

val CONFLICTS: Map<String, Conflict> = mapOf(
    "gaza" to Conflict("Guerra em Gaza", LocalDate.of(2023, 10, 7)),
    "israel" to Conflict("Guerra desde o 7 de outubro", LocalDate.of(2023, 10, 7)),
    "ucrania_russia" to Conflict("Invasão russa da Ucrânia", LocalDate.of(2022, 2, 24)),
    "sudao" to Conflict("Guerra no Sudão", LocalDate.of(2023, 4, 15)),
    "iemen" to Conflict("Houthis no controle de Sanaa", LocalDate.of(2014, 9, 21)),
    "siria" to Conflict("Síria pós-Assad", LocalDate.of(2024, 12, 8)),
)

/** Rótulo e cor do nível de tensão. */
val TENSION_LEVELS = mapOf("baixa" to 0, "moderada" to 1, "alta" to 2, "crítica" to 3)
