package com.abugdn.wid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abugdn.wid.data.CONFLICTS
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.data.FirstStats
import com.abugdn.wid.data.ORIGIN_LABELS
import com.abugdn.wid.data.RegionStat
import com.abugdn.wid.repository
import java.text.NumberFormat
import java.util.Locale

private val FIGURE_LABELS = mapOf("killed" to "Mortos", "injured" to "Feridos")

fun tensionColor(level: String): Color = when (level) {
    "crítica" -> Color(0xFFE53935)
    "alta" -> Color(0xFFFB8C00)
    "moderada" -> Color(0xFFFFB300)
    else -> Color(0xFF66BB6A)
}

@Composable
private fun InsightCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) { content() }
    }
}

@Composable
private fun CardTitle(text: String, color: Color = Red) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
}

/** Medidor 0–100 de tensão da região, com o alerta de anomalia. */
@Composable
fun TensionGauge(stat: RegionStat, modifier: Modifier = Modifier) {
    val color = tensionColor(stat.level)
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tensão ${stat.tension}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(" · ${stat.level}", style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { stat.tension / 100f },
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            modifier = Modifier.fillMaxWidth().height(8.dp),
        )
        Text(
            "${stat.last24} histórias nas últimas 24 h · média ${"%.0f".format(stat.baseline)} por dia",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (stat.spike) {
            Text(
                "⚠ Alta incomum: ritmo ${"%.1f".format(stat.spikeRatio)}× o normal nas últimas 6 h",
                style = MaterialTheme.typography.labelMedium,
                color = Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            "Combina volume, palavras de escalada (míssil, invasão, nuclear), urgência e cobertura, comparado com a média da própria região.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** "Guerra em Gaza · dia 1.084". */
@Composable
fun ConflictCounter(tag: String, modifier: Modifier = Modifier) {
    val conflict = CONFLICTS[tag] ?: return
    val day = NumberFormat.getIntegerInstance(Locale("pt", "BR")).format(conflict.day())
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text("DIA $day", style = MaterialTheme.typography.headlineSmall, color = Red, fontWeight = FontWeight.Black)
        Spacer(Modifier.width(8.dp))
        Text(
            "${conflict.label} (desde ${conflict.start.dayOfMonth}/${conflict.start.monthValue}/${conflict.start.year})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
    }
}

/** Números de mortos/feridos por veículo; destaca quando eles não batem. */
@Composable
fun FiguresCard(cluster: Cluster) {
    if (cluster.figures.isEmpty()) return
    val divergent = cluster.figures.values.any { it.divergent }
    InsightCard {
        CardTitle(if (divergent) "⚠ NÚMEROS DIVERGENTES" else "NÚMEROS CITADOS")
        cluster.figures.forEach { (kind, info) ->
            Text(
                FIGURE_LABELS[kind] ?: kind,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
            info.bySource.entries.sortedByDescending { it.value }.forEach { (source, n) ->
                Text("$n · $source", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (divergent) {
            Text(
                "Os veículos citam números diferentes. Balanços costumam subir com o tempo; confira a hora de cada um.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/** Palavras que cada origem usou para a mesma coisa. */
@Composable
fun FramingCard(cluster: Cluster) {
    if (cluster.framing.isEmpty()) return
    InsightCard {
        CardTitle("PALAVRAS DE CADA LADO")
        cluster.framing.forEach { g ->
            Text(g.group, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            g.byOrigin.forEach { (origin, terms) ->
                Text(
                    "${ORIGIN_LABELS[origin] ?: origin}: " + terms.joinToString(", ") { "“$it”" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** "Capítulo 3 de 5" da saga, com a lista de capítulos. */
@Composable
fun SagaCard(cluster: Cluster, onOpen: (String) -> Unit) {
    val saga = cluster.saga ?: return
    val context = LocalContext.current
    val repo = context.repository
    var expanded by remember(cluster.id) { mutableStateOf(false) }
    InsightCard {
        CardTitle("📚 CAPÍTULO ${saga.chapter} DE ${maxOf(saga.total, saga.chapters.size)} DESTA SAGA")
        Text(
            repo.translator.display(saga.title, saga.lang),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (expanded) {
            saga.chapters.forEachIndexed { i, ch ->
                val current = ch.clusterId == cluster.id
                Column(
                    Modifier.fillMaxWidth()
                        .clickable(enabled = !current) {
                            if (repo.cluster(ch.clusterId) != null) onOpen(ch.clusterId) else openUrl(context, ch.url)
                        }
                        .padding(vertical = 6.dp),
                ) {
                    Text(
                        "${i + 1}. ${dayClock(ch.published)} · ${ch.source}" + if (current) " · você está aqui" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (current) Red else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        repo.translator.display(ch.title, ch.lang),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
        TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Esconder capítulos" else "Ver os ${saga.chapters.size} capítulos")
        }
    }
}

/** Ranking de quem publica primeiro nas histórias grandes. */
@Composable
fun FirstRankingCard(stats: FirstStats, modifier: Modifier = Modifier) {
    if (stats.ranking.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            CardTitle("QUEM NOTICIA PRIMEIRO · 30 DIAS")
            Text(
                "Em ${stats.bigStories} histórias grandes (3+ veículos)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            stats.ranking.filter { it.firsts > 0 }.take(6).forEachIndexed { i, r ->
                Text(
                    "${i + 1}. ${r.source} — primeiro em ${r.firsts} de ${r.stories}" +
                        if (r.leadMin > 0) " · ~${r.leadMin} min antes" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
