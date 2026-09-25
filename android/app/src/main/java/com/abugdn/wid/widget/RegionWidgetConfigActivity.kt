package com.abugdn.wid.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.abugdn.wid.data.TAG_LABELS
import com.abugdn.wid.repository
import com.abugdn.wid.ui.WidTheme
import kotlinx.coroutines.launch

/** Tela aberta pelo launcher ao adicionar (ou reconfigurar) o widget: escolhe a região. */
class RegionWidgetConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Se a pessoa voltar sem escolher, o launcher cancela o widget.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        val current = widgetRegion(this, appWidgetId)

        setContent {
            WidTheme(repository.settings.value.theme) {
                Scaffold(topBar = { TopAppBar(title = { Text("Região do widget") }) }) { padding ->
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                        items(TAG_LABELS.entries.toList(), key = { it.key }) { (tag, label) ->
                            Text(
                                (if (tag == current) "● " else "") + label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth().clickable { choose(appWidgetId, tag) }
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    private fun choose(appWidgetId: Int, tag: String) {
        repository.storage.prefs.edit().putString(regionKey(appWidgetId), tag).apply()
        lifecycleScope.launch {
            RegionWidget().updateAll(this@RegionWidgetConfigActivity)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
            finish()
        }
    }
}
