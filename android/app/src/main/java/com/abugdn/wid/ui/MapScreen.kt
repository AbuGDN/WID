package com.abugdn.wid.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.abugdn.wid.data.REGION_CONTEXT
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
import org.osmdroid.views.overlay.Polygon
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import com.abugdn.wid.data.RANGES
import com.abugdn.wid.data.RangeArc

/**
 * Posição aproximada de cada região. Israel, Gaza, Cisjordânia e Líbano ficam afastados
 * do ponto real para não se empilharem no zoom de região.
 */
private val REGION_POINTS = mapOf(
    "israel" to GeoPoint(32.9, 34.6),
    "gaza" to GeoPoint(30.7, 33.8),
    "cisjordania" to GeoPoint(31.9, 36.4),
    "libano" to GeoPoint(34.6, 36.3),
    "siria" to GeoPoint(35.0, 38.5),
    "iraque" to GeoPoint(33.2, 43.7),
    "ira" to GeoPoint(32.4, 53.7),
    "iemen" to GeoPoint(15.5, 47.5),
    "ucrania_russia" to GeoPoint(49.0, 32.0),
    "sudao" to GeoPoint(15.5, 30.0),
    "otan" to GeoPoint(50.85, 4.35),
    "eua" to GeoPoint(38.9, -77.0),
    "africa" to GeoPoint(13.0, 2.0),
    "asia" to GeoPoint(28.0, 95.0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onRegion: (String) -> Unit, onOpen: (String) -> Unit) {
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
            // Sem mundo repetido nem área cinza fora do mapa.
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(80.0, -60.0, 0)
            minZoomLevel = 2.5
            maxZoomLevel = 10.0
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
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
    var showRanges by rememberSaveable { mutableStateOf(false) }
    var contextTag by remember { mutableStateOf<String?>(null) }
    contextTag?.let { RegionContextDialog(it, onOpen) { contextTag = null } }
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
            Row(Modifier.padding(horizontal = 16.dp)) {
                FilterChip(
                    selected = showRanges,
                    onClick = {
                        showRanges = !showRanges
                        if (showRanges) mapView.controller.animateTo(GeoPoint(30.0, 42.0), 3.6, 600L)
                    },
                    label = { Text("🎯 Alcance de mísseis e defesas") },
                )
            }
            // O MapView desenha fora dos próprios limites ao arrastar/dar zoom; a moldura com
            // clipChildren e o clipToBounds impedem que ele pinte por cima do resto da tela.
            AndroidView(
                factory = { ctx ->
                    (mapView.parent as? ViewGroup)?.removeView(mapView)
                    FrameLayout(ctx).apply {
                        clipChildren = true
                        clipToPadding = true
                        addView(mapView, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(0.6f).clipToBounds(),
                update = { _ ->
                    val map = mapView
                    map.overlays.removeAll { it is Marker || it is Polygon }
                    // Círculos antes dos marcadores, para os marcadores ficarem por cima e receberem o toque.
                    if (showRanges) RANGES.forEach { map.overlays.add(rangePolygon(map, it)) }
                    counts.forEach { (tag, n) ->
                        val point = REGION_POINTS[tag] ?: return@forEach
                        map.overlays.add(Marker(map).apply {
                            position = point
                            title = "${TAG_LABELS.getValue(tag)} · $n"
                            icon = countIcon(context, n)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
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
                if (showRanges) {
                    items(RANGES, key = { it.label }) { arc ->
                        RangeLegendRow(arc) { mapView.controller.animateTo(GeoPoint(arc.lat, arc.lon), rangeZoom(arc.km), 600L) }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    item {
                        Text(
                            "Alcances aproximados, de fontes abertas (até 2025). Vermelho: ataque; ouro: defesa. " +
                                "As posições das defesas são ilustrativas.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                        HorizontalDivider()
                    }
                }
                items(counts.entries.sortedByDescending { it.value }.toList(), key = { it.key }) { (tag, n) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onRegion(tag) }.padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(TAG_LABELS.getValue(tag), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (n == 1) "1 história" else "$n histórias", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            feed?.regions?.get(tag)?.let { r ->
                                Text(
                                    (if (r.spike) "⚠ " else "") + "tensão ${r.tension} · ${r.level}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tensionColor(r.level),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        if (tag in REGION_CONTEXT) {
                            IconButton(onClick = { onRegion(tag) }) {
                                Icon(Icons.Filled.Info, contentDescription = "Contexto de ${TAG_LABELS.getValue(tag)}")
                            }
                        } else {
                            Spacer(Modifier.width(48.dp))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            }
        }
    }
}

private val RANGE_RED = 0xFFB3122E.toInt()
private val RANGE_GOLD = 0xFFC9A227.toInt()

/** Círculo geodésico de alcance: vermelho para ataque, ouro para defesa. */
private fun rangePolygon(map: MapView, arc: RangeArc): Polygon = Polygon(map).apply {
    setPoints(Polygon.pointsAsCircle(GeoPoint(arc.lat, arc.lon), arc.km * 1000.0))
    val color = if (arc.defense) RANGE_GOLD else RANGE_RED
    outlinePaint.color = color
    outlinePaint.strokeWidth = 2.5f * map.context.resources.displayMetrics.density
    fillPaint.color = (color and 0x00FFFFFF) or (if (arc.defense) 0x40000000 else 0x18000000)
    title = arc.label
    // Não "engole" o toque: o mapa continua arrastável e os marcadores clicáveis.
    setOnClickListener { _, _, _ -> false }
}

private fun rangeZoom(km: Int): Double = when {
    km >= 1500 -> 3.4
    km >= 400 -> 5.0
    km >= 150 -> 6.0
    else -> 7.2
}

@Composable
private fun RangeLegendRow(arc: RangeArc, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.padding(top = 5.dp).size(10.dp)
                .background(if (arc.defense) Accent else Alert, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${arc.label} · ${"%,d".format(arc.km).replace(',', '.')} km",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(arc.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun TrendRow(label: String, values: List<Int>, highlight: Boolean, onClick: (() -> Unit)?) {
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
    val barColor = if (highlight) Accent else MaterialTheme.colorScheme.onSurfaceVariant
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
                color = if (change.startsWith("▲") || change == "novo") Accent else MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Marcador: círculo vermelho com o número de histórias da região. */
private fun countIcon(context: Context, count: Int): Drawable {
    val density = context.resources.displayMetrics.density
    val size = ((if (count >= 100) 40 else 34) * density).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val r = size / 2f
    // Ouro Argos com contorno preto e número em preto.
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC9A227.toInt() }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF050505.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    canvas.drawCircle(r, r, r - 2 * density, fill)
    canvas.drawCircle(r, r, r - 2 * density, stroke)
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF050505.toInt()
        textAlign = Paint.Align.CENTER
        textSize = 13 * density
        typeface = Typeface.DEFAULT_BOLD
    }
    canvas.drawText(count.toString(), r, r - (text.descent() + text.ascent()) / 2, text)
    return BitmapDrawable(context.resources, bitmap)
}
