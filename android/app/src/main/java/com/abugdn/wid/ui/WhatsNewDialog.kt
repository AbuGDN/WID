package com.abugdn.wid.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.abugdn.wid.data.ChangelogEntry

/**
 * Pop-up de novidades com todas as versões pendentes (quem pulou versões vê cada uma).
 * [onClose] recebe se o usuário marcou "não mostrar de novo".
 */
@Composable
fun WhatsNewDialog(entries: List<ChangelogEntry>, installedName: String, onClose: (dontShowAgain: Boolean) -> Unit) {
    var dontShow by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { onClose(dontShow) },
        confirmButton = { TextButton(onClick = { onClose(dontShow) }) { Text("Entendi") } },
        title = { Text(if (entries.size > 1) "Novidades de ${entries.size} versões" else "Novidades da versão $installedName") },
        text = {
            Column {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    entries.forEachIndexed { i, entry ->
                        if (entries.size > 1) {
                            // A mais nova usa o nome real instalado; as anteriores, o número da build.
                            val label = if (i == 0) installedName else "1.0.${entry.versionCode}"
                            Text(
                                "Versão $label",
                                style = MaterialTheme.typography.titleSmall,
                                color = Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = if (i == 0) 0.dp else 8.dp, bottom = 6.dp),
                            )
                        }
                        entry.items.forEach {
                            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 10.dp))
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { dontShow = !dontShow },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = dontShow, onCheckedChange = { dontShow = it })
                    Text("Não mostrar de novo até a próxima atualização", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
    )
}
