package com.abugdn.wid.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abugdn.wid.repository

/** Notícias salvas com a estrela: ficam no aparelho sem prazo, com o texto completo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(onOpen: (String) -> Unit) {
    val saved by LocalContext.current.repository.saved.collectAsStateWithLifecycle()
    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { TopAppBar(title = { Text("Salvos", fontWeight = FontWeight.Bold) }) },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (saved.isEmpty()) {
                item { Text("Toque na ★ dentro de uma notícia para guardá-la aqui.", modifier = Modifier.padding(24.dp)) }
            }
            items(saved, key = { it.id }) { c ->
                ClusterRow(c, onOpen)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
