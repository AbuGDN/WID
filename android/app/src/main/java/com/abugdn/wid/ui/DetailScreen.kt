package com.abugdn.wid.ui

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.abugdn.wid.data.FullText
import com.abugdn.wid.repository

private sealed interface TextState {
    data object Loading : TextState
    data class Ready(val text: FullText) : TextState
    data object Failed : TextState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(clusterId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.repository
    val feed by repo.feed.collectAsStateWithLifecycle()
    val cluster = feed?.clusters?.firstOrNull { it.id == clusterId }
    val translator = repo.translator
    var showOriginal by rememberSaveable { mutableStateOf(false) }

    val textState by produceState<TextState>(TextState.Loading, cluster?.id) {
        value = if (cluster == null) TextState.Failed
        else repo.fullText(cluster).fold({ TextState.Ready(it) }, { TextState.Failed })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cluster?.source ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        if (cluster == null) {
            Text("Notícia não encontrada (saiu do feed).", modifier = Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }
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
                Text(
                    translator.display(cluster.title, cluster.lang),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Meta(cluster)
                Spacer(Modifier.height(16.dp))

                when (val state = textState) {
                    TextState.Loading -> {
                        if (cluster.summary.isNotBlank()) {
                            Text(translator.display(cluster.summary, cluster.lang), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(16.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.padding(end = 12.dp))
                            Text("Baixando e traduzindo o texto completo…")
                        }
                    }
                    TextState.Failed -> {
                        if (cluster.summary.isNotBlank()) {
                            Text(translator.display(cluster.summary, cluster.lang), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(12.dp))
                        }
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

                if (cluster.articles.size > 1) {
                    Spacer(Modifier.height(16.dp))
                    Text("Cobertura · ${cluster.sourcesCount} veículos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    cluster.articles.forEach { a ->
                        Column(
                            Modifier.fillMaxWidth().clickable { openUrl(context, a.url) }.padding(vertical = 10.dp),
                        ) {
                            Text(translator.display(a.title, a.lang), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${a.source} · ${relativeTime(a.published)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
}
