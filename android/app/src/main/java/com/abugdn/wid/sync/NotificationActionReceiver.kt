package com.abugdn.wid.sync

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.abugdn.wid.repository

const val ACTION_SAVE = "com.abugdn.wid.SAVE"
const val ACTION_FOLLOW = "com.abugdn.wid.FOLLOW"
private const val EXTRA_ID = "cluster_id"

/** Botões "Salvar" e "Seguir" das notificações: agem sem abrir o app. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val repo = context.repository
        val cluster = repo.cluster(id)
        val message = when {
            cluster == null -> "A notícia saiu do feed."
            intent.action == ACTION_SAVE -> {
                if (!repo.isSaved(id)) repo.toggleSaved(cluster)
                "Salva nos Salvos ★"
            }
            intent.action == ACTION_FOLLOW -> {
                if (id !in repo.followed.value) repo.toggleFollow(cluster)
                "Seguindo: você será avisado de novos veículos"
            }
            else -> return
        }
        NotificationManagerCompat.from(context).cancel(id.hashCode())
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun intent(context: Context, action: String, clusterId: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                (action + clusterId).hashCode(),
                Intent(context, NotificationActionReceiver::class.java).setAction(action).putExtra(EXTRA_ID, clusterId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}
