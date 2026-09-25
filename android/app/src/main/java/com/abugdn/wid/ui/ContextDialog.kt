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
import androidx.compose.runtime.Composable
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
fun ContextDialog(title: String, text: String, milestones: List<Milestone>?, onDismiss: () -> Unit) {
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
                Spacer(Modifier.height(12.dp))
                Text(CONTEXT_DISCLAIMER, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

/** Contexto de uma região (tag), se houver texto para ela. */
@Composable
fun RegionContextDialog(tag: String, onDismiss: () -> Unit) {
    val text = REGION_CONTEXT[tag] ?: return
    ContextDialog(TAG_LABELS[tag] ?: tag, text, MILESTONES[tag], onDismiss)
}
