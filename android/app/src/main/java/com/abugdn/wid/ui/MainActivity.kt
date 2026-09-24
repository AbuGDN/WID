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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.abugdn.wid.sync.SyncWorker

const val EXTRA_CLUSTER_ID = "cluster_id"

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
            WidTheme {
                val id = openCluster
                if (id == null) {
                    HomeScreen(onOpen = { openCluster = it })
                } else {
                    BackHandler { openCluster = null }
                    DetailScreen(clusterId = id, onBack = { openCluster = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_CLUSTER_ID)?.let { openCluster = it }
    }
}
