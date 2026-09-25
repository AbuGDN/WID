package com.abugdn.wid.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.abugdn.wid.data.Cluster
import com.abugdn.wid.data.isSensitive
import com.abugdn.wid.repository
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val W = 1080
private const val H = 1350
private const val PHOTO_H = 640

/** Gera um cartão 1080×1350 (formato de post/story) da notícia e abre o compartilhar do Android. */
suspend fun shareAsImage(context: Context, cluster: Cluster) {
    val repo = context.repository
    val settings = repo.settings.value
    val title = repo.translator.display(cluster.title, cluster.lang)
    // Foto só quando não for sensível e a economia de dados estiver desligada.
    val photo = if (cluster.image != null && !settings.dataSaver && !cluster.isSensitive(repo.translator::cached)) {
        runCatching {
            val request = ImageRequest.Builder(context).data(cluster.image).allowHardware(false).size(W, PHOTO_H).build()
            (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
        }.getOrNull()
    } else {
        null
    }
    val file = withContext(Dispatchers.Default) {
        val bitmap = drawCard(cluster, title, photo)
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        File(dir, "wid-${cluster.id}.png").also { f -> f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, "$title\n${cluster.url}")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(null, uri)
    context.startActivity(Intent.createChooser(send, "Compartilhar imagem"))
}

private fun drawCard(cluster: Cluster, title: String, photo: Bitmap?): Bitmap {
    val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(0xFF121416.toInt())
    var y = 110f

    if (photo != null) {
        // Recorte central da foto na faixa de cima, com degradê para o fundo.
        val scale = maxOf(W.toFloat() / photo.width, PHOTO_H.toFloat() / photo.height)
        val srcW = (W / scale).toInt()
        val srcH = (PHOTO_H / scale).toInt()
        val left = (photo.width - srcW) / 2
        val top = (photo.height - srcH) / 2
        canvas.drawBitmap(photo, Rect(left, top, left + srcW, top + srcH), Rect(0, 0, W, PHOTO_H), Paint(Paint.FILTER_BITMAP_FLAG))
        val fade = Paint().apply {
            shader = LinearGradient(0f, PHOTO_H * 0.45f, 0f, PHOTO_H.toFloat(), 0x00121416, 0xFF121416.toInt(), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, W.toFloat(), PHOTO_H.toFloat(), fade)
        y = PHOTO_H + 30f
    }

    val red = 0xFFE53935.toInt()
    val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = red; textSize = 34f; typeface = Typeface.DEFAULT_BOLD; letterSpacing = 0.08f }
    canvas.drawText("WID · NOTÍCIAS DE GUERRA", 60f, y, label)
    y += 40f

    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF2F2F2.toInt(); textSize = if (photo != null) 62f else 76f; typeface = Typeface.DEFAULT_BOLD }
    val maxLines = if (photo != null) 6 else 9
    val layout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, W - 120)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setLineSpacing(0f, 1.1f)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()
    canvas.save()
    canvas.translate(60f, y)
    layout.draw(canvas)
    canvas.restore()
    y += layout.height + 60f

    val date = runCatching {
        DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault()).format(Instant.parse(cluster.updated))
    }.getOrDefault("")
    val meta = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9AA0A6.toInt(); textSize = 36f }
    canvas.drawText(
        "${cluster.source} · ${cluster.sourcesCount} ${if (cluster.sourcesCount == 1) "veículo" else "veículos"} · $date",
        60f, y, meta,
    )

    val bar = Paint().apply { color = red }
    canvas.drawRect(60f, H - 120f, 180f, H - 112f, bar)
    val footer = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF9AA0A6.toInt(); textSize = 30f }
    canvas.drawText("Compartilhado pelo app WID", 60f, H - 64f, footer)
    return bitmap
}
