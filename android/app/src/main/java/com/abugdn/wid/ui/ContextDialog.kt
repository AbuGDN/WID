package com.abugdn.wid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abugdn.wid.data.Actor
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.data.SOURCE_DISCLAIMER
import com.abugdn.wid.data.SOURCE_PROFILES
import com.abugdn.wid.data.related
import com.abugdn.wid.repository
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abugdn.wid.data.CONTEXT_DISCLAIMER
import com.abugdn.wid.data.MILESTONES
import com.abugdn.wid.data.Milestone
import com.abugdn.wid.data.REGION_CONTEXT
import com.abugdn.wid.data.TAG_LABELS

/** Cartão de contexto: explicação e, para regiões, os marcos históricos. */
@Composable
fun ContextDialog(
    title: String,
    text: String,
    milestones: List<Milestone>?,
    related: List<Cluster> = emptyList(),
    onOpen: ((String) -> Unit)? = null,
    footer: String = CONTEXT_DISCLAIMER,
    onDismiss: () -> Unit,
) {
    val translator = LocalContext.current.repository.translator
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(text)
                if (!milestones.isNullOrEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Marcos", style = MaterialTheme.typography.titleSmall, color = Red, fontWeight = FontWeight.Bold)
                    milestones.forEach { m ->
                        Row(Modifier.padding(top = 6.dp)) {
                            Text(m.date, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(84.dp))
                            Text(m.text, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (related.isNotEmpty() && onOpen != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Notícias recentes", style = MaterialTheme.typography.titleSmall, color = Red, fontWeight = FontWeight.Bold)
                    related.forEach { c ->
                        Text(
                            translator.display(c.title, c.lang),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth().clickable { onDismiss(); onOpen(c.id) }.padding(vertical = 6.dp),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(footer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

/** Contexto de uma região (tag), se houver texto para ela. */
@Composable
fun RegionContextDialog(tag: String, onOpen: ((String) -> Unit)? = null, exclude: String? = null, onDismiss: () -> Unit) {
    val text = REGION_CONTEXT[tag] ?: return
    val feed by LocalContext.current.repository.feed.collectAsStateWithLifecycle()
    val related = feed?.clusters.orEmpty().filter { tag in it.tags && it.id != exclude }.take(5)
    ContextDialog(TAG_LABELS[tag] ?: tag, text, MILESTONES[tag], related = related, onOpen = onOpen, onDismiss = onDismiss)
}

/** Contexto de um ator, pessoa ou arma, com as notícias recentes que o citam. */
@Composable
fun ActorContextDialog(actor: Actor, onOpen: ((String) -> Unit)?, exclude: String? = null, onDismiss: () -> Unit) {
    val repo = LocalContext.current.repository
    val feed by repo.feed.collectAsStateWithLifecycle()
    val related = remember(actor.key, feed) { actor.related(feed?.clusters.orEmpty(), repo.translator::cached, exclude) }
    ContextDialog(actor.name, actor.text, null, related = related, onOpen = onOpen, onDismiss = onDismiss)
}

/** Perfil de um veículo: país, dono e linha editorial. */
@Composable
fun SourceProfileDialog(source: String, onDismiss: () -> Unit) {
    val text = SOURCE_PROFILES[source] ?: "Sem perfil cadastrado para este veículo."
    ContextDialog(source, text, null, footer = SOURCE_DISCLAIMER, onDismiss = onDismiss)
}
