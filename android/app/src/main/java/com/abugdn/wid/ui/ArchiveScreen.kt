package com.abugdn.wid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.abugdn.wid.data.weekTop
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abugdn.wid.repository
import kotlinx.coroutines.launch

/** A principal de cada dia, do mais recente para o mais antigo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(onOpen: (String) -> Unit) {
    val repo = LocalContext.current.repository
    val archive by repo.archive.collectAsStateWithLifecycle()
    val translator = repo.translator
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    fun load() = scope.launch {
        loading = true
        error = repo.loadArchive().isFailure
        loading = false
    }
    LaunchedEffect(Unit) { if (archive == null) load() }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Arquivo · principal de cada dia", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        PullToRefreshBox(isRefreshing = loading, onRefresh = { load() }, modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                if (error) item { Text("Sem conexão.", color = Red, modifier = Modifier.padding(16.dp)) }
                if (archive.isNullOrEmpty() && !loading) {
                    item { Text("Nada no arquivo ainda. O servidor guarda um dia por vez a partir de 24/09/2026.", modifier = Modifier.padding(24.dp)) }
                }
                val week = weekTop(archive.orEmpty())
                if (week.size >= 2) {
                    item {
                        Card(
                            modifier = Modifier.padding(16.dp, 8.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("RESUMO DA SEMANA", style = MaterialTheme.typography.labelMedium, color = Red, fontWeight = FontWeight.Bold)
                                week.forEachIndexed { i, day ->
                                    Column(Modifier.fillMaxWidth().clickable { onOpen(day.top.id) }.padding(vertical = 8.dp)) {
                                        Text(
                                            "${i + 1}. " + translator.display(day.top.title, day.top.lang),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            "${dayLabel(day.date)} · ${day.top.source} · ${day.top.sourcesCount} veículos",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                items(archive.orEmpty(), key = { it.date }) { day ->
                    Column {
                        Text(
                            dayLabel(day.date).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = Red,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp),
                        )
                        ClusterRow(day.top, onOpen)
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }
}
