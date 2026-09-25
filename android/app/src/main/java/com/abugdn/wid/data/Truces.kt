package com.abugdn.wid.data

import java.time.LocalDate

/** Cessar-fogo acompanhado pelo contador. Datas de início até 2025. */
data class Truce(val key: String, val label: String, val tags: Set<String>, val start: LocalDate)

val TRUCES = listOf(
    Truce("gaza_2025", "Cessar-fogo em Gaza", setOf("gaza"), LocalDate.of(2025, 10, 10)),
    Truce("libano_2024", "Cessar-fogo Israel–Hezbollah", setOf("libano"), LocalDate.of(2024, 11, 27)),
    Truce("ira_2025", "Cessar-fogo Israel–Irã", setOf("ira"), LocalDate.of(2025, 6, 24)),
)

const val TRUCE_DISCLAIMER =
    "Datas de início até 2025. Se esta trégua já acabou, toque em “Trégua encerrada” para esconder o contador."
