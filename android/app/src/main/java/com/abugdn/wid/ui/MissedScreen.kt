package com.abugdn.wid.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abugdn.wid.repository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Depois de mais de 24 h fora: as principais do período em que a pessoa não abriu o app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissedScreen(since: Long, onClose: () -> Unit, onOpen: (String) -> Unit) {
    val repo = LocalContext.current.repository
    val feed by repo.feed.collectAsStateWithLifecycle()
    val archive by repo.archive.collectAsStateWithLifecycle()
    val sinceInstant = Instant.ofEpochMilli(since)
    val days = java.time.Duration.between(sinceInstant, Instant.now()).toDays().coerceAtLeast(1)
    // Mais de 2 dias fora: o feed (48 h) não cobre tudo, então usa também o arquivo.
    LaunchedEffect(Unit) { if (days >= 2 && repo.archive.value == null) repo.loadArchive() }

    val recent = feed?.clusters.orEmpty()
        .filter { runCatching { Instant.parse(it.published).isAfter(sinceInstant) }.getOrDefault(false) }
        .sortedByDescending { it.dayScore }
        .take(8)
    val sinceDate = sinceInstant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
    val older = archive.orEmpty()
        .filter { it.date >= sinceDate && it.date < LocalDate.now().minusDays(1).toString() && recent.none { c -> c.id == it.top.id } }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("O que você perdeu", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    "Você ficou ${if (days == 1L) "1 dia" else "$days dias"} sem abrir o Argos. As principais do período:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(recent, key = { it.id }) { c ->
                ClusterRow(c, onOpen)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (older.isNotEmpty()) {
                item {
                    Text(
                        "DIAS ANTERIORES",
                        style = MaterialTheme.typography.labelMedium,
                        color = Accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                    )
                }
                items(older, key = { "d-" + it.date }) { day ->
                    Text(dayLabel(day.date), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 16.dp, top = 8.dp))
                    ClusterRow(day.top, onOpen)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            item {
                Button(onClick = onClose, modifier = Modifier.padding(16.dp)) { Text("Continuar para o app") }
            }
        }
    }
}
