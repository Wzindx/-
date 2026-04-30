package com.yang.emperor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream


fun decodePreviewBitmap(bytes: ByteArray, maxSide: Int = 1600) =
    BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)

        var sample = 1
        while ((outWidth / sample) > maxSide || (outHeight / sample) > maxSide) {
            sample *= 2
        }

        inJustDecodeBounds = false
        inSampleSize = sample.coerceAtLeast(1)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, this)
    }
fun readReferenceImageBytes(context: Context, imageUri: Uri): ByteArray {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(imageUri)?.use { input ->
        BitmapFactory.decodeStream(input, null, bounds)
    } ?: error("无法读取参考图")

    val width = bounds.outWidth
    val height = bounds.outHeight
    if (width <= 0 || height <= 0) {
        return readAllBytes(context, imageUri)
    }

    val maxDimension = 2048
    var sample = 1
    while (maxOf(width, height) / sample > maxDimension) {
        sample *= 2
    }

    val originalSize = runCatching {
        resolver.openFileDescriptor(imageUri, "r")?.use { it.statSize }
    }.getOrNull() ?: -1L

    if (sample <= 1 && originalSize in 0..(6L * 1024L * 1024L)) {
        return readAllBytes(context, imageUri)
    }

    val bitmap = resolver.openInputStream(imageUri)?.use { input ->
        BitmapFactory.decodeStream(
            input,
            null,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
        )
    } ?: return readAllBytes(context, imageUri)

    return ByteArrayOutputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)
        bitmap.recycle()
        output.toByteArray()
    }
}
fun readAllBytes(context: Context, imageUri: Uri): ByteArray =
    context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
        ?: error("无法读取参考图")
fun buildCompactImageDataUrl(
    originalBytes: ByteArray,
    maxSide: Int = 1536,
    jpegQuality: Int = 86,
    maxEncodedBytes: Int = 6 * 1024 * 1024
): String {
    val bounds = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, bounds)

    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "参考图格式无法识别" }

    var sampleSize = 1
    while ((bounds.outWidth / sampleSize) > maxSide || (bounds.outHeight / sampleSize) > maxSide) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize.coerceAtLeast(1)
        inPreferredConfig = Bitmap.Config.RGB_565
    }

    val bitmap = BitmapFactory.decodeByteArray(originalBytes, 0, originalBytes.size, options)
        ?: error("参考图解码失败")

    val output = ByteArrayOutputStream()
    try {
        bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(60, 95), output)
    } finally {
        bitmap.recycle()
    }

    val compactBytes = output.toByteArray()
    require(compactBytes.size <= maxEncodedBytes) {
        "参考图过大，已阻止以避免内存溢出。请先裁剪或压缩图片后再使用 Generations 兼容模式。"
    }

    return "data:image/jpeg;base64," + Base64.encodeToString(compactBytes, Base64.NO_WRAP)
}
