package com.abugdn.wid.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.remember
import com.abugdn.wid.data.CONTEXT_DISCLAIMER
import com.abugdn.wid.data.ORIGIN_LABELS
import com.abugdn.wid.data.REGION_CONTEXT
import com.abugdn.wid.data.TAG_LABELS
import com.abugdn.wid.data.actors
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.abugdn.wid.data.ArticleRef
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.data.FullText
import com.abugdn.wid.repository

private sealed interface TextState {
    data object Loading : TextState
    data class Ready(val text: FullText) : TextState
    data object Failed : TextState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(cluster: Cluster, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.repository
    val saved by repo.saved.collectAsStateWithLifecycle()
    val isSaved = saved.any { it.id == cluster.id }
    val followed by repo.followed.collectAsStateWithLifecycle()
    val isFollowed = cluster.id in followed
    val translator = repo.translator
    var showOriginal by rememberSaveable { mutableStateOf(false) }

    val textState by produceState<TextState>(TextState.Loading, cluster.id) {
        value = repo.fullText(cluster).fold({ TextState.Ready(it) }, { TextState.Failed })
    }
    val title = translator.display(cluster.title, cluster.lang)
    LaunchedEffect(cluster.id) { repo.markRead(cluster.id) }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text(cluster.source) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { repo.toggleFollow(cluster) }) {
                        Icon(
                            Icons.Filled.Notifications,
                            contentDescription = if (isFollowed) "Deixar de seguir" else "Seguir história",
                            tint = if (isFollowed) Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { repo.toggleSaved(cluster) }) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = if (isSaved) "Remover dos salvos" else "Salvar",
                            tint = if (isSaved) Red else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { share(context, title, cluster.url) }) {
                        Icon(Icons.Filled.Share, contentDescription = "Compartilhar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            cluster.image?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
            }
            Column(Modifier.padding(16.dp)) {
                if (cluster.urgent) {
                    Text("URGENTE", color = Red, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Meta(cluster)
                if (isFollowed) {
                    Text(
                        "Seguindo: você será avisado quando outros veículos noticiarem.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Red,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                ContextChips(cluster)
                Spacer(Modifier.height(16.dp))

                when (val state = textState) {
                    TextState.Loading -> {
                        Summary(cluster)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.padding(end = 12.dp))
                            Text("Baixando e traduzindo o texto completo…")
                        }
                    }
                    TextState.Failed -> {
                        Summary(cluster)
                        Text(
                            "Não foi possível baixar o texto completo (sem conexão ou paywall).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    is TextState.Ready -> {
                        val text = state.text
                        val translated = text.translated
                        val paragraphs = if (translated != null && !showOriginal) translated else text.paragraphs
                        Text(
                            "Texto: ${text.source}" + if (translated != null) " · traduzido automaticamente" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (translated != null) {
                            TextButton(onClick = { showOriginal = !showOriginal }) {
                                Text(if (showOriginal) "Ver tradução" else "Ver original")
                            }
                        }
                        paragraphs.forEach {
                            Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 12.dp))
                        }
                    }
                }

                OutlinedButton(onClick = { openUrl(context, cluster.url) }, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Abrir no site (${cluster.source})")
                }

                Perspectives(cluster)

                if (cluster.articles.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Linha do tempo · ${cluster.sourcesCount} veículos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    val ordered = cluster.articles.sortedBy { it.published }
                    ordered.forEachIndexed { i, a ->
                        TimelineItem(a, first = i == 0, last = i == ordered.lastIndex) { openUrl(context, a.url) }
                    }
                }
            }
        }
    }
}

/** Chips "ⓘ Hamas", "ⓘ Gaza"… que abrem um cartão de contexto. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextChips(cluster: Cluster) {
    val translator = LocalContext.current.repository.translator
    val actors = remember(cluster.id) { cluster.actors(translator::cached) }
    val regions = cluster.tags.filter { it in REGION_CONTEXT }
    if (actors.isEmpty() && regions.isEmpty()) return
    var open by remember { mutableStateOf<Pair<String, String>?>(null) }

    FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        actors.forEach { a ->
            AssistChip(onClick = { open = a.name to a.text }, label = { Text("ⓘ ${a.name}") })
        }
        regions.forEach { tag ->
            val label = TAG_LABELS[tag] ?: tag
            AssistChip(onClick = { open = label to REGION_CONTEXT.getValue(tag) }, label = { Text("ⓘ $label") })
        }
    }
    open?.let { (title, text) ->
        AlertDialog(
            onDismissRequest = { open = null },
            confirmButton = { TextButton(onClick = { open = null }) { Text("Fechar") } },
            title = { Text(title) },
            text = {
                Column {
                    Text(text)
                    Spacer(Modifier.height(12.dp))
                    Text(CONTEXT_DISCLAIMER, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
        )
    }
}

/** "Como cada lado noticiou": títulos agrupados pela origem do veículo. */
@Composable
private fun Perspectives(cluster: Cluster) {
    val translator = LocalContext.current.repository.translator
    val context = LocalContext.current
    val groups = ORIGIN_LABELS.keys.associateWith { origin -> cluster.articles.filter { it.origin == origin } }
        .filterValues { it.isNotEmpty() }
    if (groups.size < 2) return

    Spacer(Modifier.height(16.dp))
    Text("Como cada lado noticiou", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    groups.forEach { (origin, articles) ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(ORIGIN_LABELS.getValue(origin).uppercase(), style = MaterialTheme.typography.labelSmall, color = Red, fontWeight = FontWeight.Bold)
                articles.distinctBy { it.source }.forEach { a ->
                    Column(Modifier.fillMaxWidth().clickable { openUrl(context, a.url) }.padding(vertical = 6.dp)) {
                        Text(translator.display(a.title, a.lang), style = MaterialTheme.typography.bodyMedium)
                        Text(a.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(cluster: Cluster) {
    if (cluster.summary.isBlank()) return
    Text(
        LocalContext.current.repository.translator.display(cluster.summary, cluster.lang),
        style = MaterialTheme.typography.bodyLarge,
    )
    Spacer(Modifier.height(12.dp))
}

/** Um ponto na linha do tempo: horário, veículo e título, ligados por uma linha vertical. */
@Composable
private fun TimelineItem(a: ArticleRef, first: Boolean, last: Boolean, onClick: () -> Unit) {
    val translator = LocalContext.current.repository.translator
    val line = MaterialTheme.colorScheme.outlineVariant
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).clickable(onClick = onClick)) {
        Box(Modifier.width(20.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
                Box(Modifier.width(2.dp).height(14.dp).background(if (first) MaterialTheme.colorScheme.surface else line))
                Box(Modifier.size(10.dp).clip(CircleShape).background(if (first) Red else line))
                Box(Modifier.width(2.dp).weight(1f).background(if (last) MaterialTheme.colorScheme.surface else line))
            }
        }
        Column(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 12.dp)) {
            Text(
                "${dayClock(a.published)} · ${a.source}" + if (first) " · primeiro a noticiar" else "",
                style = MaterialTheme.typography.labelSmall,
                color = if (first) Red else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(translator.display(a.title, a.lang), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingScreen(onBack: () -> Unit) {
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("WID") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
    ) { padding ->
        Text("Carregando… (se não aparecer, a notícia saiu do feed)", modifier = Modifier.padding(padding).padding(24.dp))
    }
}

fun openUrl(context: Context, url: String) {
    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
}

private fun share(context: Context, title: String, url: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, "$title\n$url")
    context.startActivity(Intent.createChooser(send, "Compartilhar notícia"))
}
