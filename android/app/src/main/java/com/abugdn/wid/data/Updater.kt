package com.abugdn.wid.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings as AndroidSettings
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request

private const val LATEST_RELEASE = "https://api.github.com/repos/AbuGDN/WID/releases/latest"
private const val APK_MIME = "application/vnd.android.package-archive"
private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

@Serializable
private data class GhRelease(
    @SerialName("tag_name") val tag: String,
    @SerialName("html_url") val htmlUrl: String,
    val assets: List<GhAsset> = emptyList(),
)

@Serializable
private data class GhAsset(val name: String, @SerialName("browser_download_url") val url: String)

data class AppUpdate(val versionCode: Long, val versionName: String, val apkUrl: String, val pageUrl: String)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Downloading : UpdateState
    /** Falta o usuário permitir "instalar apps desconhecidos" para o WID. */
    data object NeedsPermission : UpdateState
    data class Failed(val message: String) : UpdateState
}

/** Procura versão nova nos Releases do GitHub e instala baixando o APK. */
class Updater(private val context: Context) {
    private val prefs = context.getSharedPreferences("wid", Context.MODE_PRIVATE)
    private val http = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).build()

    val installedCode: Long = PackageInfoCompat.getLongVersionCode(
        context.packageManager.getPackageInfo(context.packageName, 0)
    )
    val installedName: String = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"

    private val _available = MutableStateFlow<AppUpdate?>(null)
    val available: StateFlow<AppUpdate?> = _available.asStateFlow()

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /**
     * Consulta o último release. Com [force] = false respeita um intervalo de 6 h
     * (a API do GitHub sem login permite 60 consultas por hora).
     */
    suspend fun check(force: Boolean = false): Result<AppUpdate?> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong("update_checked_at", 0) < CHECK_INTERVAL_MS) {
            return@withContext Result.success(_available.value)
        }
        runCatching {
            val request = Request.Builder().url(LATEST_RELEASE)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "WID-app")
                .build()
            val body = http.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
                resp.body?.string() ?: throw IOException("resposta vazia")
            }
            val release = json.decodeFromString<GhRelease>(body)
            // Tags são "v1.0.<número da build>", e o número da build é o versionCode.
            val code = release.tag.substringAfterLast('.').toLongOrNull() ?: throw IOException("tag inesperada: ${release.tag}")
            val apk = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: throw IOException("release sem APK")
            prefs.edit().putLong("update_checked_at", now).apply()
            val update = if (code > installedCode) {
                AppUpdate(code, release.tag.removePrefix("v"), apk.url, release.htmlUrl)
            } else null
            _available.value = update
            update
        }
    }

    /** Verdadeiro uma única vez por versão nova (para notificar sem repetir). */
    fun shouldNotify(update: AppUpdate): Boolean {
        if (prefs.getLong("update_notified", 0) >= update.versionCode) return false
        prefs.edit().putLong("update_notified", update.versionCode).apply()
        return true
    }

    /** Baixa o APK com o DownloadManager e abre o instalador do Android ao terminar. */
    fun install(update: AppUpdate) {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            _state.value = UpdateState.NeedsPermission
            context.startActivity(
                Intent(AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val dm = context.getSystemService(DownloadManager::class.java)
        val fileName = "WID-${update.versionName}.apk"
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.listFiles()?.forEach { it.delete() }
        val id = runCatching {
            dm.enqueue(
                DownloadManager.Request(Uri.parse(update.apkUrl))
                    .setTitle("WID ${update.versionName}")
                    .setMimeType(APK_MIME)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            )
        }.getOrElse {
            _state.value = UpdateState.Failed("Não foi possível iniciar o download")
            return
        }
        _state.value = UpdateState.Downloading

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) != id) return
                context.unregisterReceiver(this)
                val uri = dm.getUriForDownloadedFile(id)
                if (uri == null) {
                    _state.value = UpdateState.Failed("O download falhou")
                    return
                }
                _state.value = UpdateState.Idle
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, APK_MIME)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.onFailure { _state.value = UpdateState.Failed("Não foi possível abrir o instalador") }
            }
        }
        // O aviso de download concluído vem do app de downloads do sistema, por isso EXPORTED.
        ContextCompat.registerReceiver(
            context, receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED,
        )
    }

    fun resetState() {
        _state.value = UpdateState.Idle
    }
}
