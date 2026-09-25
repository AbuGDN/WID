package com.abugdn.wid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.repository
import java.time.Instant
import kotlinx.coroutines.launch

/** 5 cartões × 12 s = o dia em 1 minuto. */
private const val STORY_COUNT = 5
private const val STORY_MS = 12_000

/** As principais das últimas 24 h, começando pela principal do dia. */
fun storyItems(clusters: List<Cluster>, top: Cluster?): List<Cluster> {
    val cutoff = Instant.now().minusSeconds(24 * 3600)
    val recent = clusters
        .filter { runCatching { Instant.parse(it.updated).isAfter(cutoff) }.getOrDefault(true) }
        .sortedByDescending { it.dayScore }
    return (listOfNotNull(top) + recent).distinctBy { it.id }.take(STORY_COUNT)
}

@Composable
fun StoryScreen(onClose: () -> Unit, onOpen: (String) -> Unit) {
    val repo = LocalContext.current.repository
    val feed by repo.feed.collectAsStateWithLifecycle()
    val items = remember(feed) { storyItems(feed?.clusters.orEmpty(), feed?.topOfDay) }
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val pager = rememberPagerState { items.size }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // Cada cartão corre 12 s e passa sozinho; ao fim do último, fecha.
    LaunchedEffect(pager.currentPage) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(STORY_MS, easing = LinearEasing))
        if (pager.currentPage < items.lastIndex) pager.animateScrollToPage(pager.currentPage + 1) else onClose()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            StoryPage(
                cluster = items[page],
                index = page,
                total = items.size,
                modifier = Modifier.pointerInput(page) {
                    // Toque no terço esquerdo volta; no resto, avança.
                    detectTapGestures { offset ->
                        scope.launch {
                            when {
                                offset.x < size.width / 3 && page > 0 -> pager.animateScrollToPage(page - 1)
                                offset.x >= size.width / 3 && page < items.lastIndex -> pager.animateScrollToPage(page + 1)
                                offset.x >= size.width / 3 -> onClose()
                            }
                        }
                    }
                },
            )
        }
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp, 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items.indices.forEach { i ->
                    LinearProgressIndicator(
                        progress = {
                            when {
                                i < pager.currentPage -> 1f
                                i == pager.currentPage -> progress.value
                                else -> 0f
                            }
                        },
                        modifier = Modifier.weight(1f).height(3.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "O DIA EM 1 MINUTO",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Fechar", tint = Color.White) }
            }
        }
        Button(
            onClick = { onOpen(items[pager.currentPage].id) },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 20.dp),
        ) { Text("Ler a notícia") }
    }
}

@Composable
private fun StoryPage(cluster: Cluster, index: Int, total: Int, modifier: Modifier) {
    val translator = LocalContext.current.repository.translator
    Box(modifier.fillMaxSize()) {
        cluster.image?.let {
            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.35f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.95f))
            )
        )
        Column(
            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 88.dp),
        ) {
            Text(
                "${index + 1}/$total" + if (index == 0) " · PRINCIPAL DO DIA" else "",
                color = Red,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                translator.display(cluster.title, cluster.lang),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            if (cluster.summary.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    translator.display(cluster.summary, cluster.lang),
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "${cluster.source} · ${cluster.sourcesCount} veículos · ${relativeTime(cluster.updated)}",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
