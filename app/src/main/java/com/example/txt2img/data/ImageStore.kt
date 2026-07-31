package com.example.txt2img.data

import android.content.Context
import java.io.File

/**
 * 生成图片的文件存储：内部存储 files/images/ 目录，随应用生命周期持久。
 */
class ImageStore(private val context: Context) {

    private val dir: File
        get() = File(context.filesDir, "images").apply { if (!exists()) mkdirs() }

    fun save(bytes: ByteArray): String? = try {
        val name = "img_${System.currentTimeMillis()}.png"
        File(dir, name).writeBytes(bytes)
        name
    } catch (e: Exception) {
        null
    }

    fun saveRef(bytes: ByteArray, mime: String): String? = try {
        val ext = when {
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("gif") -> "gif"
            else -> "jpg"
        }
        val name = "ref_${System.currentTimeMillis()}.$ext"
        File(dir, name).writeBytes(bytes)
        name
    } catch (e: Exception) {
        null
    }

    fun delete(name: String) {
        try {
            File(dir, name).delete()
        } catch (e: Exception) {
            // 忽略删除失败
        }
    }

    fun fileFor(name: String): File = File(dir, name)
}
