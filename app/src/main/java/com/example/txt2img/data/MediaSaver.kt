package com.example.txt2img.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * 保存图片到手机相册。
 * API 29+：MediaStore（无需权限）；API 26-28：写入公共 Pictures 目录（需 WRITE_EXTERNAL_STORAGE，调用方先请求权限）。
 */
object MediaSaver {

    fun save(context: Context, src: File, displayName: String): Boolean = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveScoped(context, src, displayName)
        } else {
            saveLegacy(context, src, displayName)
        }
    } catch (e: Exception) {
        false
    }

    private fun saveScoped(context: Context, src: File, displayName: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeOf(displayName))
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/文字生图")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri: Uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        val wrote = resolver.openOutputStream(uri)?.use { out ->
            src.inputStream().use { ins -> ins.copyTo(out) }
        } != null
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return wrote
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, src: File, displayName: String): Boolean {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "文字生图",
        )
        if (!dir.exists()) dir.mkdirs()
        val dst = File(dir, displayName)
        src.inputStream().use { ins -> dst.outputStream().use { outs -> ins.copyTo(outs) } }
        context.sendBroadcast(
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dst)),
        )
        return true
    }

    private fun mimeOf(name: String): String = when {
        name.endsWith(".webp", true) -> "image/webp"
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        name.endsWith(".gif", true) -> "image/gif"
        else -> "image/png"
    }
}
