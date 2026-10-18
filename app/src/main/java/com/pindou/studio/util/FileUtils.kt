package com.pindou.studio.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 文件与媒体相关工具。 */
object FileUtils {

    /** 把 Uri 内容复制到缓存目录，返回本地文件。 */
    fun copyUriToCache(context: Context, uri: Uri, fileName: String): File? {
        return try {
            val dir = File(context.cacheDir, "imports").apply { mkdirs() }
            val target = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            if (target.exists() && target.length() > 0) target else null
        } catch (e: Exception) {
            null
        }
    }

    /** 生成一个基于时间的文件名。 */
    fun timestampName(prefix: String, ext: String): String {
        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return prefix + "_" + fmt.format(Date()) + "." + ext
    }

    /**
     * 保存 PNG 到相册（Pictures/拼豆工坊）。
     * @return 保存后的 Uri，失败返回 null。
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, displayName: String, subDir: String = "拼豆工坊"): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" + subDir)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                uri
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    subDir,
                )
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, displayName)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(file.absolutePath), arrayOf("image/png"), null,
                )
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 保存任意文件到下载目录。 */
    fun saveFileToDownloads(context: Context, source: File, displayName: String, mime: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/拼豆工坊")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        source.inputStream().use { input -> input.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                uri
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "拼豆工坊",
                )
                if (!dir.exists()) dir.mkdirs()
                val target = File(dir, displayName)
                source.copyTo(target, overwrite = true)
                Uri.fromFile(target)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 通过 FileProvider 生成可分享的 Uri。 */
    fun shareUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)

    /** 把位图写入缓存并返回文件。 */
    fun writeTempPng(context: Context, bitmap: Bitmap, name: String): File {
        val dir = File(context.cacheDir, "export").apply { mkdirs() }
        val file = File(dir, name)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return file
    }

    /** 按最大边长采样解码图片，避免大图 OOM。 */
    fun decodeSampledBitmap(context: Context, uri: Uri, maxSide: Int = 2048): Bitmap? {
        return try {
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, bounds)
            }
            var sample = 1
            var side = maxOf(bounds.outWidth, bounds.outHeight)
            while (side / sample > maxSide * 2) sample *= 2
            val opts = android.graphics.BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            var bmp = context.contentResolver.openInputStream(uri)?.use {
                android.graphics.BitmapFactory.decodeStream(it, null, opts)
            } ?: return null
            val longSide = maxOf(bmp.width, bmp.height)
            if (longSide > maxSide) {
                val scale = maxSide.toFloat() / longSide
                val scaled = Bitmap.createScaledBitmap(
                    bmp,
                    (bmp.width * scale).toInt().coerceAtLeast(1),
                    (bmp.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
                if (scaled != bmp) bmp.recycle()
                bmp = scaled
            }
            bmp
        } catch (e: Exception) {
            null
        } catch (t: Throwable) {
            null
        }
    }

    fun formatDate(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        return fmt.format(Date(millis))
    }
}
