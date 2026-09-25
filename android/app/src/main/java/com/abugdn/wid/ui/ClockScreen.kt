package com.abugdn.wid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abugdn.wid.data.TAG_LABELS
import com.abugdn.wid.data.TRUCES
import com.abugdn.wid.repository

/** Relógio do Argos: índice global, histórico de 30 dias, regiões por tensão e tréguas. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockScreen(onBack: () -> Unit, onRegion: (String) -> Unit) {
    val repo = LocalContext.current.repository
    val feed by repo.feed.collectAsStateWithLifecycle()
    val stats by repo.stats.collectAsStateWithLifecycle()
    val ended by repo.endedTruces.collectAsStateWithLifecycle()
    val vigil by repo.vigil.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { if (repo.stats.value == null) repo.loadStats() }

    val days = stats?.days.orEmpty().takeLast(30)
    val globalDays = days.filter { it.global > 0 }.map { it.date to it.global }
    val regions = feed?.regions.orEmpty().entries.sortedByDescending { it.value.tension }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Relógio do Argos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                feed?.global?.let { ArgosClock(it) }
                Text(
                    "Tensão global de 0 a 100: 60% da região mais tensa + 40% da média das três mais tensas. " +
                        "Avisa quando sobe de faixa (baixa, moderada, alta, crítica).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "RELÓGIO · 30 DIAS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                )
                TensionHistoryChart(globalDays)
                Text(
                    "REGIÕES AGORA",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
            }
            items(regions, key = { it.key }) { (tag, r) ->
                val history = days.mapNotNull { d -> d.tension[tag] }
                Row(
                    Modifier.fillMaxWidth().clickable { onRegion(tag) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(TAG_LABELS[tag] ?: tag, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            (if (r.spike) "⚠ " else "") + "tensão ${r.tension} · ${r.level}" +
                                if (history.isNotEmpty()) " · pico 30 d: ${history.max()}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = tensionColor(r.level),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (history.size >= 2) {
                        TensionHistoryChart(history.mapIndexed { i, v -> i.toString() to v }.takeLast(14), Modifier.width(120.dp), compact = true)
                    }
                }
                HorizontalDivider()
            }
            item {
                Text(
                    "TRÉGUAS",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 20.dp),
                )
                TRUCES.forEach { truce ->
                    TruceCard(truce, truce.key in ended, vigil) { repo.setTruceEnded(truce.key, it) }
                }
            }
        }
    }
}
