package com.abugdn.wid.ui

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayTime = DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(ZoneId.systemDefault())
private val time = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

fun relativeTime(iso: String, now: Instant = Instant.now()): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
    val minutes = Duration.between(instant, now).toMinutes()
    return when {
        minutes < 1 -> "agora"
        minutes < 60 -> "há $minutes min"
        minutes < 24 * 60 -> "há ${minutes / 60} h"
        else -> dayTime.format(instant)
    }
}

fun clockTime(iso: String): String =
    runCatching { time.format(Instant.parse(iso)) }.getOrDefault("")
