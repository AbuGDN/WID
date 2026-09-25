package com.abugdn.wid.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.data.Feed
import com.abugdn.wid.data.Translator
import com.abugdn.wid.repository
import com.abugdn.wid.ui.EXTRA_CLUSTER_ID
import com.abugdn.wid.ui.MainActivity
import com.abugdn.wid.ui.relativeTime

private val BG = Color(0xFF050505)
private val GOLD = Color(0xFFC9A227)
private val WHITE = Color(0xFFE8E2D0)
private val GREY = Color(0xFF8A8578)

private val SMALL = DpSize(180.dp, 110.dp)
private val LARGE = DpSize(250.dp, 200.dp)

private val clusterKey = ActionParameters.Key<String>(EXTRA_CLUSTER_ID)

class TopWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.repository
        val feed = repo.feed.value ?: repo.storage.loadFeed()
        provideContent { WidgetBody(feed, repo.translator) }
    }
}

class TopWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TopWidget()
}

@Composable
private fun WidgetBody(feed: Feed?, translator: Translator) {
    val top = feed?.topOfDay
    val base = GlanceModifier.fillMaxSize().background(BG).cornerRadius(16.dp).padding(14.dp)
    if (top == null) {
        Column(modifier = base.clickable(actionStartActivity<MainActivity>())) {
            Text("ARGOS", style = TextStyle(color = ColorProvider(GOLD), fontWeight = FontWeight.Bold, fontSize = 12.sp))
            Text("Abra o app para carregar as notícias", style = TextStyle(color = ColorProvider(WHITE), fontSize = 14.sp))
        }
        return
    }
    val open = actionStartActivity<MainActivity>(actionParametersOf(clusterKey to top.id))
    Column(modifier = base.clickable(open)) {
        Text(
            "PRINCIPAL DO DIA",
            style = TextStyle(color = ColorProvider(GOLD), fontWeight = FontWeight.Bold, fontSize = 11.sp),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            translator.display(top.title, top.lang),
            style = TextStyle(color = ColorProvider(WHITE), fontWeight = FontWeight.Bold, fontSize = 15.sp),
            maxLines = 4,
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(meta(top), style = TextStyle(color = ColorProvider(GREY), fontSize = 11.sp), maxLines = 1)

        if (LocalSize.current.height >= LARGE.height) {
            val others = feed.clusters.filter { it.id != top.id }.sortedByDescending { it.dayScore }.take(2)
            for (c in others) {
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    translator.display(c.title, c.lang),
                    style = TextStyle(color = ColorProvider(WHITE), fontSize = 13.sp),
                    maxLines = 2,
                    modifier = GlanceModifier.fillMaxWidth().clickable(
                        actionStartActivity<MainActivity>(actionParametersOf(clusterKey to c.id))
                    ),
                )
                Text(meta(c), style = TextStyle(color = ColorProvider(GREY), fontSize = 10.sp), maxLines = 1)
            }
        }
    }
}

private fun meta(c: Cluster): String {
    val count = if (c.sourcesCount > 1) " · ${c.sourcesCount} veículos" else ""
    return "${c.source}$count · ${relativeTime(c.updated)}"
}
