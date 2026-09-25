package com.abugdn.wid.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abugdn.wid.repository
import com.abugdn.wid.sync.SyncWorker

const val EXTRA_CLUSTER_ID = "cluster_id"

/** As telas internas não aplicam insets do sistema: a barra de baixo é do Scaffold externo. */
val NoInsets = WindowInsets(0, 0, 0, 0)

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Hoje", Icons.Filled.Home),
    MAP("Mapa", Icons.Filled.Place),
    ARCHIVE("Arquivo", Icons.Filled.DateRange),
    SAVED("Salvos", Icons.Filled.Star),
}

class MainActivity : ComponentActivity() {
    private var openCluster by mutableStateOf<String?>(null)

    private val askNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        openCluster = intent.getStringExtra(EXTRA_CLUSTER_ID)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        if (savedInstanceState == null) SyncWorker.runNow(this)

        setContent {
            val settings by repository.settings.state.collectAsStateWithLifecycle()
            WidTheme(settings.theme, settings.textScale) {
                App(openCluster = openCluster, onOpenCluster = { openCluster = it })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        repository.startVisit()
    }

    override fun onStop() {
        super.onStop()
        repository.endVisit()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_CLUSTER_ID)?.let { openCluster = it }
    }
}

@Composable
private fun App(openCluster: String?, onOpenCluster: (String?) -> Unit) {
    val repo = LocalContext.current.repository
    // Coletados para que a busca da notícia aberta se refaça quando os dados chegarem.
    val feed by repo.feed.collectAsStateWithLifecycle()
    val saved by repo.saved.collectAsStateWithLifecycle()
    val archive by repo.archive.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var tag by rememberSaveable { mutableStateOf<String?>(null) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var storyOpen by rememberSaveable { mutableStateOf(false) }
    // Aparece a cada abertura até a pessoa marcar "não mostrar de novo".
    var whatsNewOpen by rememberSaveable { mutableStateOf(repo.shouldShowWhatsNew()) }
    if (whatsNewOpen) {
        WhatsNewDialog(repo.updater.installedName) { dontShowAgain ->
            if (dontShowAgain) repo.dismissWhatsNew()
            whatsNewOpen = false
        }
    }

    val detail = remember(openCluster, feed, saved, archive) { openCluster?.let(repo::cluster) }

    Scaffold(
        bottomBar = {
            if (openCluster == null && !settingsOpen && !storyOpen) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        // O "dia em 1 minuto" ocupa a tela inteira, inclusive atrás da barra de navegação.
        Box(Modifier.padding(bottom = if (storyOpen) 0.dp else padding.calculateBottomPadding())) {
            when {
                openCluster != null -> {
                    BackHandler { onOpenCluster(null) }
                    if (detail != null) {
                        DetailScreen(cluster = detail, onBack = { onOpenCluster(null) })
                    } else {
                        MissingScreen(onBack = { onOpenCluster(null) })
                    }
                }
                storyOpen -> {
                    BackHandler { storyOpen = false }
                    StoryScreen(onClose = { storyOpen = false }, onOpen = { storyOpen = false; onOpenCluster(it) })
                }
                settingsOpen -> {
                    BackHandler { settingsOpen = false }
                    SettingsScreen(onBack = { settingsOpen = false })
                }
                else -> {
                    if (tab != Tab.HOME) BackHandler { tab = Tab.HOME }
                    when (tab) {
                        Tab.HOME -> HomeScreen(
                            tag = tag,
                            onTag = { tag = it },
                            onOpen = { onOpenCluster(it) },
                            onSettings = { settingsOpen = true },
                            onStory = { storyOpen = true },
                        )
                        Tab.MAP -> MapScreen(onRegion = { tag = it; tab = Tab.HOME })
                        Tab.ARCHIVE -> ArchiveScreen(onOpen = { onOpenCluster(it) })
                        Tab.SAVED -> SavedScreen(onOpen = { onOpenCluster(it) })
                    }
                }
            }
        }
    }
}
