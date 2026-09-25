package com.abugdn.wid.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.data.isSensitive
import com.abugdn.wid.repository

/**
 * Foto da notícia. Some com a economia de dados; em notícias com mortos ou feridos
 * aparece borrada até a pessoa tocar ([revealable] = false só borra, sem revelar).
 */
@Composable
fun NewsImage(cluster: Cluster, modifier: Modifier, revealable: Boolean = true) {
    val url = cluster.image ?: return
    if (LocalDataSaver.current) return
    val repo = LocalContext.current.repository
    val settings by repo.settings.state.collectAsStateWithLifecycle()
    val sensitive = settings.blurSensitive && remember(cluster.id) { cluster.isSensitive(repo.translator::cached) }
    var revealed by rememberSaveable(cluster.id) { mutableStateOf(false) }
    val hidden = sensitive && !revealed

    Box(modifier.then(if (hidden && revealable) Modifier.clickable { revealed = true } else Modifier)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().then(if (hidden) Modifier.blur(32.dp) else Modifier),
        )
        if (hidden) {
            // O blur só existe no Android 12+; antes disso, um véu escuro faz o papel.
            val veil = if (Build.VERSION.SDK_INT >= 31) 0.35f else 0.9f
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = veil)))
            if (revealable) {
                Text(
                    "Imagem sensível\ntoque para ver",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(8.dp),
                )
            }
        }
    }
}
