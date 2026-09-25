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
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.data.TAG_LABELS
import com.abugdn.wid.repository
import com.abugdn.wid.ui.EXTRA_CLUSTER_ID
import com.abugdn.wid.ui.MainActivity
import com.abugdn.wid.ui.relativeTime
import java.time.Instant

private const val DEFAULT_REGION = "israel"

fun regionKey(appWidgetId: Int) = "region_widget_$appWidgetId"

fun widgetRegion(context: Context, appWidgetId: Int): String =
    context.repository.storage.prefs.getString(regionKey(appWidgetId), DEFAULT_REGION) ?: DEFAULT_REGION

/** A história de maior peso das últimas 24 h numa região. */
fun topOfRegion(clusters: List<Cluster>, tag: String): Cluster? {
    val cutoff = Instant.now().minusSeconds(24 * 3600)
    val inRegion = clusters.filter { tag in it.tags }
    return inRegion.filter { runCatching { Instant.parse(it.updated).isAfter(cutoff) }.getOrDefault(true) }
        .maxByOrNull { it.dayScore } ?: inRegion.maxByOrNull { it.dayScore }
}

/** Widget com a principal de uma região escolhida (Israel, EUA, Ucrânia...). */
class RegionWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = context.repository
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val tag = widgetRegion(context, appWidgetId)
        val feed = repo.feed.value ?: repo.storage.loadFeed()
        val top = feed?.let { topOfRegion(it.clusters, tag) }
        val label = TAG_LABELS[tag] ?: tag
        val title = top?.let { repo.translator.display(it.title, it.lang) }
        val tension = feed?.regions?.get(tag)?.let { "Tensão ${it.tension} · ${it.level}" + if (it.spike) " · ⚠ alta incomum" else "" }
        provideContent { RegionBody(label, top, title, tension) }
    }
}

class RegionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RegionWidget()
}

@Composable
private fun RegionBody(label: String, top: Cluster?, title: String?, tension: String?) {
    val action = if (top != null) {
        actionStartActivity<MainActivity>(actionParametersOf(ActionParameters.Key<String>(EXTRA_CLUSTER_ID) to top.id))
    } else {
        actionStartActivity<MainActivity>()
    }
    Column(
        GlanceModifier.fillMaxSize().background(Color(0xFF15181B)).cornerRadius(16.dp).padding(14.dp).clickable(action),
    ) {
        Text(
            label.uppercase() + " · PRINCIPAL",
            style = TextStyle(color = ColorProvider(Color(0xFFE53935)), fontWeight = FontWeight.Bold, fontSize = 11.sp),
        )
        if (tension != null) {
            Text(tension, style = TextStyle(color = ColorProvider(Color(0xFFFFB300)), fontSize = 10.sp), maxLines = 1)
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(
            title ?: "Nenhuma notícia de $label nas últimas 48 h",
            style = TextStyle(color = ColorProvider(Color(0xFFF2F2F2)), fontWeight = FontWeight.Bold, fontSize = 15.sp),
            maxLines = 4,
        )
        if (top != null) {
            Spacer(GlanceModifier.height(4.dp))
            Text(
                "${top.source} · ${top.sourcesCount} veículos · ${relativeTime(top.updated)}",
                style = TextStyle(color = ColorProvider(Color(0xFF9AA0A6)), fontSize = 11.sp),
                maxLines = 1,
            )
        }
    }
}
