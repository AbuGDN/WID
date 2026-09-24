package com.abugdn.wid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abugdn.wid.data.UpdateState
import com.abugdn.wid.repository

/** Cartão "versão nova disponível" com o botão de atualizar. Some quando não há versão nova. */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val updater = context.repository.updater
    val update by updater.available.collectAsStateWithLifecycle()
    val state by updater.state.collectAsStateWithLifecycle()
    val available = update ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Nova versão ${available.versionName} disponível",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            val hint = when (val s = state) {
                UpdateState.Idle -> "Você está na ${updater.installedName}."
                UpdateState.Downloading -> "Baixando… o instalador abre sozinho ao terminar."
                UpdateState.NeedsPermission -> "Permita \"instalar apps desconhecidos\" para o WID e toque em Atualizar de novo."
                is UpdateState.Failed -> "${s.message}. Tente de novo ou baixe pelo navegador."
            }
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { updater.install(available) },
                    enabled = state != UpdateState.Downloading,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Atualizar") }
                TextButton(onClick = { openUrl(context, available.pageUrl) }) {
                    Text("Ver no GitHub", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}
