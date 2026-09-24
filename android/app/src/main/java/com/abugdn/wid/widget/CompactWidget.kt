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
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.abugdn.wid.data.Feed
import com.abugdn.wid.data.Translator
import com.abugdn.wid.repository
import com.abugdn.wid.ui.EXTRA_CLUSTER_ID
import com.abugdn.wid.ui.MainActivity
import com.abugdn.wid.ui.relativeTime

/** Widget 4x1: só a manchete da principal do dia. */
class CompactWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.repository
        val feed = repo.feed.value ?: repo.storage.loadFeed()
        provideContent { CompactBody(feed, repo.translator) }
    }
}

class CompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CompactWidget()
}

@Composable
private fun CompactBody(feed: Feed?, translator: Translator) {
    val top = feed?.topOfDay
    val action = if (top != null) {
        actionStartActivity<MainActivity>(actionParametersOf(ActionParameters.Key<String>(EXTRA_CLUSTER_ID) to top.id))
    } else {
        actionStartActivity<MainActivity>()
    }
    Row(
        modifier = GlanceModifier.fillMaxSize().background(Color(0xFF15181B)).cornerRadius(16.dp)
            .padding(horizontal = 14.dp, vertical = 6.dp).clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("●", style = TextStyle(color = ColorProvider(Color(0xFFE53935)), fontSize = 14.sp))
        Spacer(GlanceModifier.width(8.dp))
        Column {
            Text(
                top?.let { translator.display(it.title, it.lang) } ?: "Abra o WID para carregar",
                style = TextStyle(color = ColorProvider(Color(0xFFF2F2F2)), fontWeight = FontWeight.Bold, fontSize = 13.sp),
                maxLines = 2,
            )
            if (top != null) {
                Text(
                    "${top.source} · ${relativeTime(top.updated)}",
                    style = TextStyle(color = ColorProvider(Color(0xFF9AA0A6)), fontSize = 10.sp),
                    maxLines = 1,
                )
            }
        }
    }
}
