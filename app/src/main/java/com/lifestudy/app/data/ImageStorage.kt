package com.lifestudy.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

object ImageStorage {

    /**
     * 把用户选中的图片 Uri (content://) 拷贝到 App 内部存储, 返回可用于 Coil 的 file:// URL.
     * 复制是必要的, 因为系统图库返回的 Uri 只有短期读权限, 进程退出后就失效.
     */
    fun saveImage(context: Context, uri: Uri): String? = runCatching {
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val name = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg"
        val dest = File(dir, name)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: return@runCatching null
        Uri.fromFile(dest).toString()
    }.getOrNull()
}
