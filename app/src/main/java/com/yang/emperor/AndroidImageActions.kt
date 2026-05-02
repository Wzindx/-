package com.yang.emperor

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import java.io.IOException
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val IMAGE_NOTIFICATION_CHANNEL_ID = "image_generation_result"
private const val IMAGE_NOTIFICATION_ID_BASE = 1001
private const val IMAGE_SAVE_DIRECTORY = "ImageForge"

fun ensureImageNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val channel = NotificationChannel(
        IMAGE_NOTIFICATION_CHANNEL_ID,
        "图片生成结果",
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = "图片生成完成后的提示"
    }

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}

@SuppressLint("MissingPermission")
fun notifyImageReady(context: Context, imageUri: String) {
    if (!canPostNotifications(context)) return

    val openIntent = buildOpenImageIntent(context, imageUri)
    val pendingIntent = PendingIntent.getActivity(
        context,
        IMAGE_NOTIFICATION_ID_BASE,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, IMAGE_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_gallery)
        .setContentTitle("ImageForge 图片已完成")
        .setContentText(
            if (imageUri.startsWith("content://")) {
                "已保存到图片记录，点击可打开系统查看。"
            } else {
                "已写入图片记录，请回到应用查看结果。"
            }
        )
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                if (imageUri.startsWith("content://")) {
                    "图片已生成并保存到图片记录。点击通知可用系统应用查看；也可以回到 ImageForge 的图片记录页继续查看描述、打开或分享。"
                } else {
                    "图片已生成并写入图片记录。请回到 ImageForge 的图片记录页查看结果。"
                }
            )
        )
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context).notify(
        (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
        notification
    )
}

fun shareImageFromHistory(context: Context, imageUri: String): Boolean {
    if (!imageUri.startsWith("content://")) {
        Toast.makeText(context, "没有可分享的图片文件。", Toast.LENGTH_SHORT).show()
        return false
    }

    val uri = imageUri.toUri()
    return try {
        context.contentResolver.openInputStream(uri)?.close()
            ?: error("无法读取图片文件")
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = context.contentResolver.getType(uri) ?: "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, "generated_image", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "分享图片").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(chooser)
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "没有可用的分享应用。", Toast.LENGTH_SHORT).show()
        false
    } catch (e: Exception) {
        Toast.makeText(context, "分享失败：${e.message ?: "图片无法读取"}", Toast.LENGTH_LONG).show()
        false
    }
}

fun openImageFromHistory(context: Context, imageUri: String): Boolean {
    if (!imageUri.startsWith("content://")) {
        Toast.makeText(context, "没有可打开的图片文件。", Toast.LENGTH_SHORT).show()
        return false
    }

    val uri = imageUri.toUri()
    return try {
        context.contentResolver.openInputStream(uri)?.close()
            ?: error("无法读取图片文件")
        context.startActivity(buildOpenImageIntent(context, imageUri))
        true
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "没有可用的图片查看应用。", Toast.LENGTH_SHORT).show()
        false
    } catch (e: Exception) {
        Toast.makeText(context, "打开失败：${e.message ?: "图片无法读取"}", Toast.LENGTH_LONG).show()
        false
    }
}

fun saveExistingImageToGallery(
    context: Context,
    imageUri: String,
    customDirectoryUri: Uri? = null
): String {
    require(imageUri.startsWith("content://")) { "没有可保存的图片文件" }
    val uri = imageUri.toUri()
    val mimeType = context.contentResolver.getType(uri).orEmpty()
    val format = when {
        mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> "jpg"
        mimeType.contains("webp", ignoreCase = true) -> "webp"
        else -> "png"
    }
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("无法读取图片文件")
    return saveToGallery(context, bytes, format, customDirectoryUri)
}

fun saveToGallery(
    context: Context,
    bytes: ByteArray,
    format: String,
    customDirectoryUri: Uri? = null
): String {
    require(bytes.isNotEmpty()) { "图片数据为空，无法保存" }

    val normalizedFormat = normalizeImageFormat(format)
    val name = "image_${System.currentTimeMillis()}.${normalizedFormat.extension}"

    if (customDirectoryUri != null) {
        return saveToCustomDirectory(
            context = context,
            bytes = bytes,
            name = name,
            mimeType = normalizedFormat.mimeType,
            directoryUri = customDirectoryUri
        )
    }

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, normalizedFormat.mimeType)
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/$IMAGE_SAVE_DIRECTORY"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("无法创建图片文件")

    try {
        resolver.openOutputStream(uri)?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: error("无法打开图片输出流")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, publishValues, null, null)
        }

        return uri.toString()
    } catch (e: Exception) {
        runCatching { resolver.delete(uri, null, null) }
        throw e
    }
}

private fun saveToCustomDirectory(
    context: Context,
    bytes: ByteArray,
    name: String,
    mimeType: String,
    directoryUri: Uri
): String {
    val resolver = context.contentResolver

    var createdImageUri: Uri? = null
    try {
        val targetDirectoryUri = if (DocumentsContract.isTreeUri(directoryUri)) {
            val treeDocumentId = DocumentsContract.getTreeDocumentId(directoryUri)
            DocumentsContract.buildDocumentUriUsingTree(directoryUri, treeDocumentId)
        } else {
            directoryUri
        }

        val imageUri = DocumentsContract.createDocument(
            resolver,
            targetDirectoryUri,
            mimeType,
            name
        ) ?: throw IOException("无法在自定义保存目录中创建图片文件：$directoryUri")
        createdImageUri = imageUri

        resolver.openOutputStream(imageUri, "w")?.use { output ->
            output.write(bytes)
            output.flush()
        } ?: throw IOException("无法打开自定义保存目录的图片输出流：$imageUri")

        return imageUri.toString()
    } catch (e: SecurityException) {
        createdImageUri?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
        throw IOException("没有自定义保存目录的写入权限，请重新选择保存目录并授权：$directoryUri", e)
    } catch (e: IllegalArgumentException) {
        createdImageUri?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
        throw IOException("自定义保存目录 URI 无效：$directoryUri。请重新选择保存目录。", e)
    } catch (e: IOException) {
        createdImageUri?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
        throw e
    } catch (e: Exception) {
        createdImageUri?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } }
        throw IOException("保存到自定义目录失败：$directoryUri", e)
    }
}

private fun canPostNotifications(context: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}

private fun buildOpenImageIntent(context: Context, imageUri: String): Intent {
    if (imageUri.startsWith("content://")) {
        val uri = imageUri.toUri()
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            clipData = ClipData.newUri(context.contentResolver, "generated_image", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    return Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

private data class ImageFormat(
    val extension: String,
    val mimeType: String
)

private fun normalizeImageFormat(format: String): ImageFormat {
    return when (format.lowercase()) {
        "jpg", "jpeg" -> ImageFormat(extension = "jpg", mimeType = "image/jpeg")
        "webp" -> ImageFormat(extension = "webp", mimeType = "image/webp")
        else -> ImageFormat(extension = "png", mimeType = "image/png")
    }
}