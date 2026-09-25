package com.abugdn.wid.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abugdn.wid.data.buildBulletin
import com.abugdn.wid.repository
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Boletim semanal do Argos: a imagem da semana, pronta para compartilhar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulletinScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = context.repository
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        val days = repo.loadArchive(days = 8, publish = false).getOrNull() ?: repo.archive.value.orEmpty()
        val first = repo.first.value ?: repo.loadFirst().getOrNull()
        val bulletin = buildBulletin(days, repo.feed.value, repo.vigil.value, first, LocalDate.now())
        bulletin.biggestAlert?.takeIf { it.lang != "pt" }?.let { repo.translator.translateAll(listOf(it.title)) }
        bitmap = withContext(Dispatchers.Default) { drawBulletin(bulletin, repo.translator) }
    }

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = {
            TopAppBar(
                title = { Text("Boletim semanal", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    val image = bitmap
                    if (image != null) {
                        IconButton(onClick = {
                            scope.launch { shareBitmap(context, image, "argos-boletim-${LocalDate.now()}", "Boletim semanal do Argos") }
                        }) { Icon(Icons.Filled.Share, contentDescription = "Compartilhar") }
                    }
                },
            )
        },
    ) { padding ->
        val image = bitmap
        if (image == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Image(
                    image.asImageBitmap(),
                    contentDescription = "Boletim semanal",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1080f / 1350f),
                )
                Text(
                    "Gerado com os últimos 7 dias do arquivo e do seu registro de vigília. Todo domingo chega um aviso quando o boletim fica pronto.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
