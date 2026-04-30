package com.yang.emperor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val IMAGE_NOTIFICATION_CHANNEL_ID = "image_generation_result"

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
fun notifyImageReady(context: Context, imageUri: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    val openIntent = Intent(Intent.ACTION_VIEW).apply {
        if (imageUri.startsWith("content://")) {
            setDataAndType(Uri.parse(imageUri), "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            setPackage(context.packageName)
        }
    }

    val pendingIntent = PendingIntent.getActivity(
        context,
        1001,
        openIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, IMAGE_NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_gallery)
        .setContentTitle("图片生成完成")
        .setContentText(if (imageUri.startsWith("content://")) "已保存到相册，可点击查看。" else "可回到应用查看结果。")
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
}
fun shareImageFromHistory(context: Context, imageUri: String) {
    if (!imageUri.startsWith("content://")) return

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(shareIntent, "分享图片")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
fun saveToGallery(context: Context, bytes: ByteArray, format: String): String {
    val ext = if (format == "jpeg") "jpg" else format
    val name = "image_${System.currentTimeMillis()}.$ext"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/$ext")
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES + "/UniversalImageStudio"
        )
    }
    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        values
    ) ?: error("无法创建图片文件")
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
    return uri.toString()
}
