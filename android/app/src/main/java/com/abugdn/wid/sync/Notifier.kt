package com.abugdn.wid.sync

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.abugdn.wid.R
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.data.Feed
import com.abugdn.wid.repository
import com.abugdn.wid.ui.EXTRA_CLUSTER_ID
import com.abugdn.wid.ui.MainActivity

object Notifier {
    private const val CHANNEL_URGENT = "urgent"
    private const val CHANNEL_TOP = "top"
    private const val TOP_MIN_INTERVAL_MS = 4 * 60 * 60 * 1000L

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_URGENT, context.getString(R.string.channel_urgent), NotificationManager.IMPORTANCE_HIGH)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TOP, context.getString(R.string.channel_top), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /**
     * Notifica histórias urgentes ainda não vistas e a troca da principal do dia
     * (no máximo a cada 4 h). Na primeira sincronização só marca tudo como visto.
     */
    fun handle(context: Context, feed: Feed) {
        val prefs = context.repository.storage.prefs
        val notified = prefs.getStringSet("notified", emptySet())!!.toMutableSet()
        val firstRun = !prefs.getBoolean("initialized", false)
        val urgent = feed.clusters.filter { it.urgent && it.id !in notified }

        if (!firstRun) urgent.forEach { notify(context, CHANNEL_URGENT, "Urgente", it) }
        notified += urgent.map { it.id }
        notified.retainAll(feed.clusters.map { it.id }.toSet())

        val top = feed.topOfDay
        val editor = prefs.edit().putStringSet("notified", notified).putBoolean("initialized", true)
        if (top != null && top.id != prefs.getString("top_id", null)) {
            val now = System.currentTimeMillis()
            if (!firstRun && now - prefs.getLong("top_at", 0) >= TOP_MIN_INTERVAL_MS && top.id !in urgent.map { it.id }) {
                notify(context, CHANNEL_TOP, "Principal do dia", top)
            }
            editor.putString("top_id", top.id).putLong("top_at", now)
        }
        editor.apply()
    }

    @SuppressLint("MissingPermission") // checado logo abaixo
    private fun notify(context: Context, channel: String, label: String, cluster: Cluster) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val title = context.repository.translator.display(cluster.title, cluster.lang)
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_CLUSTER_ID, cluster.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            context, cluster.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$label · ${cluster.sourcesCount} veículos")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(cluster.id.hashCode(), notification)
    }
}
