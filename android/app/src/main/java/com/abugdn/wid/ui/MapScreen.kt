package com.abugdn.wid.ui

import android.view.MotionEvent
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

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Mapa · últimas 48 h", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
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
