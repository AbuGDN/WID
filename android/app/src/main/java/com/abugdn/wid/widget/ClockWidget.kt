package com.abugdn.wid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.abugdn.wid.data.GlobalClock
import com.abugdn.wid.data.TAG_LABELS
import com.abugdn.wid.repository
import com.abugdn.wid.ui.EXTRA_SHORTCUT
import com.abugdn.wid.ui.MainActivity

/** Widget 2x2: o Relógio do Argos (tensão global 0–100). Toque abre a tela do relógio. */
class ClockWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.repository
        val feed = repo.feed.value ?: repo.storage.loadFeed()
        provideContent { ClockBody(feed?.global) }
    }
}

class ClockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ClockWidget()
}

private fun levelColor(level: String): Color = when (level) {
    "crítica" -> Color(0xFFB3122E)
    "alta" -> Color(0xFFC8662B)
    "moderada" -> Color(0xFFC9A227)
    else -> Color(0xFF6E8B6A)
}

@Composable
private fun ClockBody(clock: GlobalClock?) {
    val action = actionStartActivity<MainActivity>(actionParametersOf(ActionParameters.Key<String>(EXTRA_SHORTCUT) to "clock"))
    Column(
        modifier = GlanceModifier.fillMaxSize().background(Color(0xFF050505)).cornerRadius(16.dp)
            .padding(12.dp).clickable(action),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("👁 RELÓGIO DO ARGOS", style = TextStyle(color = ColorProvider(Color(0xFFC9A227)), fontSize = 10.sp, fontWeight = FontWeight.Bold))
        if (clock == null) {
            Text("Abra o Argos", style = TextStyle(color = ColorProvider(Color(0xFF8A8578)), fontSize = 12.sp))
        } else {
            val color = ColorProvider(levelColor(clock.level))
            Text("${clock.index}", style = TextStyle(color = color, fontSize = 44.sp, fontWeight = FontWeight.Bold))
            Text(clock.level.uppercase(), style = TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            Text(
                TAG_LABELS[clock.leader] ?: clock.leader,
                style = TextStyle(color = ColorProvider(Color(0xFF8A8578)), fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}
