package com.abugdn.wid.ui

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abugdn.wid.data.TAG_LABELS
import com.abugdn.wid.repository
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/** Posição aproximada de cada região no mapa (Gaza/Cisjordânia levemente afastadas de Israel). */
private val REGION_POINTS = mapOf(
    "israel" to GeoPoint(32.3, 34.95),
    "gaza" to GeoPoint(31.35, 34.3),
    "cisjordania" to GeoPoint(31.95, 35.35),
    "libano" to GeoPoint(33.9, 35.8),
    "siria" to GeoPoint(35.0, 38.5),
    "iraque" to GeoPoint(33.2, 43.7),
    "ira" to GeoPoint(32.4, 53.7),
    "iemen" to GeoPoint(15.5, 47.5),
    "ucrania_russia" to GeoPoint(49.0, 32.0),
    "sudao" to GeoPoint(15.5, 30.0),
    "otan" to GeoPoint(50.85, 4.35),
    "africa" to GeoPoint(13.0, 2.0),
    "asia" to GeoPoint(28.0, 95.0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onRegion: (String) -> Unit) {
    val context = LocalContext.current
    val feed by context.repository.feed.collectAsStateWithLifecycle()
    val counts = remember(feed) {
        TAG_LABELS.keys.associateWith { tag -> feed?.clusters.orEmpty().count { tag in it.tags } }
            .filterValues { it > 0 }
    }

    val mapView = remember {
        // O OpenStreetMap exige um user-agent identificando o app.
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            // Cache de tiles na pasta privada do app (sem pedir permissão de armazenamento).
            osmdroidBasePath = java.io.File(context.cacheDir, "osmdroid")
            osmdroidTileCache = java.io.File(osmdroidBasePath, "tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(4.0)
            controller.setCenter(GeoPoint(30.0, 42.0))
            // Deixa o mapa receber arrastos sem a tela rolar junto.
            setOnTouchListener { v, e ->
                if (e.action == MotionEvent.ACTION_DOWN) v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDetach() }
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text(if (tab == 0) "Mapa · últimas 48 h" else "Tendência por região", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Mapa") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Tendência") })
            }
            if (tab == 1) {
                TrendBody(onRegion)
            } else {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxWidth().weight(0.6f),
                update = { map ->
                    map.overlays.removeAll { it is Marker }
                    counts.forEach { (tag, n) ->
                        val point = REGION_POINTS[tag] ?: return@forEach
                        map.overlays.add(Marker(map).apply {
                            position = point
                            title = "${TAG_LABELS.getValue(tag)} · $n"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            setOnMarkerClickListener { m, _ ->
                                if (m.isInfoWindowShown) onRegion(tag) else m.showInfoWindow()
                                true
                            }
                        })
                    }
                    map.invalidate()
                },
            )
            Text(
                "Toque no marcador para ver o nome; toque de novo (ou na lista) para abrir as notícias.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp, 8.dp),
            )
            LazyColumn(Modifier.weight(0.4f)) {
                items(counts.entries.sortedByDescending { it.value }.toList(), key = { it.key }) { (tag, n) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onRegion(tag) }.padding(16.dp, 12.dp),
                    ) {
                        Text(TAG_LABELS.getValue(tag), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Text(if (n == 1) "1 história" else "$n histórias", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            }
        }
    }
}

private const val TREND_DAYS = 14

/**
 * Termômetro: histórias iniciadas por dia em cada região (últimos 14 dias), com a
 * variação da última semana contra a anterior.
 */
@Composable
private fun TrendBody(onRegion: (String) -> Unit) {
    val repo = LocalContext.current.repository
    val stats by repo.stats.collectAsStateWithLifecycle()
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { failed = repo.loadStats().isFailure && stats == null }

    val days = stats?.days.orEmpty().takeLast(TREND_DAYS)
    if (days.isEmpty()) {
        Text(
            if (failed) "Sem conexão." else "Carregando… (o servidor começou a contar em 25/09/2026; o gráfico enche com os dias)",
            modifier = Modifier.padding(24.dp),
        )
        return
    }
    val rows = TAG_LABELS.keys.map { tag -> tag to days.map { it.counts[tag] ?: 0 } }
        .filter { (_, values) -> values.sum() > 0 }
        .sortedByDescending { (_, values) -> values.takeLast(7).sum() }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            TrendRow("Todas as regiões", days.map { it.total }, highlight = true, onClick = null)
            Text(
                "Histórias novas por dia, ${dayLabel(days.first().date)} a ${dayLabel(days.last().date)}. A seta compara os últimos 7 dias com os 7 anteriores.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp),
            )
            HorizontalDivider()
        }
        items(rows, key = { it.first }) { (tag, values) ->
            TrendRow(TAG_LABELS.getValue(tag), values, highlight = false) { onRegion(tag) }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun TrendRow(label: String, values: List<Int>, highlight: Boolean, onClick: (() -> Unit)?) {
    val last = values.takeLast(7).sum()
    val prev = values.dropLast(7).takeLast(7).sum()
    val change = when {
        prev == 0 && last == 0 -> ""
        prev == 0 -> "novo"
        else -> {
            val pct = (last - prev) * 100 / prev
            when {
                pct > 0 -> "▲ $pct%"
                pct < 0 -> "▼ ${-pct}%"
                else -> "= 0%"
            }
        }
    }
    val barColor = if (highlight) Red else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal)
            Text(
                "$last na semana" + if (change.isNotEmpty()) " · $change" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (change.startsWith("▲") || change == "novo") Red else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
        Canvas(Modifier.width(140.dp).height(36.dp)) {
            val slot = size.width / values.size
            val barWidth = slot * 0.7f
            values.forEachIndexed { i, v ->
                val h = if (v == 0) 1.dp.toPx() else size.height * v / max
                drawRect(
                    color = barColor,
                    topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h),
                    size = Size(barWidth, h),
                )
            }
        }
    }
}
