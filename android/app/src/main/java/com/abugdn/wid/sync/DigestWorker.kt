package com.abugdn.wid.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.abugdn.wid.repository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Uma vez por dia, no horário escolhido: notificação com as 3 principais das últimas 24 h. */
class DigestWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = applicationContext.repository
        if (!repo.settings.value.dailyDigest) return Result.success()
        // Sem internet, usa o feed guardado.
        val feed = repo.refresh().getOrNull() ?: repo.feed.value ?: return Result.success()
        Notifier.digest(applicationContext, feed)
        return Result.success()
    }

    companion object {
        private const val NAME = "digest"

        /** (Re)agenda para a próxima ocorrência de [hour]:00. Chamado no início e ao mudar os ajustes. */
        fun schedule(context: Context) {
            val settings = context.repository.settings.value
            val work = WorkManager.getInstance(context)
            if (!settings.dailyDigest) {
                work.cancelUniqueWork(NAME)
                return
            }
            val now = LocalDateTime.now()
            var next = now.withHour(settings.digestHour).withMinute(0).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<DigestWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(Duration.between(now, next).toMinutes(), TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
                .addTag("digest-h${settings.digestHour}")
                .build()
            work.enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
