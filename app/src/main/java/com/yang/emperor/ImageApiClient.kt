package com.yang.emperor

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID


fun endpoint(baseUrl: String, path: String): String {
    val b = baseUrl.trim().trimEnd('/')
    return if (b.endsWith("/v1")) "$b$path" else "$b/v1$path"
}
fun callGenerate(
    baseUrl: String,
    apiKey: String,
    model: String,
    prompt: String,
    n: Int,
    size: String,
    quality: String
): ByteArray {
    require(apiKey.isNotBlank()) { "请填写 API Key" }
    require(prompt.isNotBlank()) { "请填写 Prompt" }

    val body = JSONObject()
        .put("model", model.trim())
        .put("prompt", prompt)
        .put("n", n.coerceIn(1, 10))
        .put("size", size)
        .put("quality", quality)

    val conn = URL(endpoint(baseUrl, "/images/generations")).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 30000
    conn.readTimeout = 180000
    conn.doOutput = true
    conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
    conn.setRequestProperty("Content-Type", "application/json")
    conn.outputStream.use { it.write(body.toString().toByteArray()) }
    return parseImageResponse(conn)
}
fun callEdit(
    baseUrl: String,
    apiKey: String,
    model: String,
    prompt: String,
    imageBytes: ByteArray?,
    size: String,
    quality: String,
    outputFormat: String,
    background: String
): ByteArray {
    require(apiKey.isNotBlank()) { "请填写 API Key" }
    require(prompt.isNotBlank()) { "请填写编辑指令" }
    val sourceImageBytes = requireNotNull(imageBytes) { "无法读取参考图，请重新选择图片后再试" }

    val boundary = "----AndroidBoundary${UUID.randomUUID()}"
    val conn = URL(endpoint(baseUrl, "/images/edits")).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 30000
    conn.readTimeout = 180000
    conn.doOutput = true
    conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

    conn.outputStream.use { out ->
        fun writeText(value: String) {
            out.write(value.toByteArray(Charsets.UTF_8))
        }

        fun field(name: String, value: String) {
            writeText("--$boundary\r\n")
            writeText("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
            writeText("$value\r\n")
        }

        field("model", model.trim())
        field("prompt", prompt)
        field("size", size)
        field("quality", quality)
        field("output_format", outputFormat)
        if (background.isNotBlank()) field("background", background)

        writeText("--$boundary\r\n")
        writeText("Content-Disposition: form-data; name=\"image\"; filename=\"image.png\"\r\n")
        writeText("Content-Type: image/png\r\n\r\n")
        out.write(sourceImageBytes)
        writeText("\r\n--$boundary--\r\n")
    }
    return parseImageResponse(conn)
}
fun callEditGenerationsCompat(
    baseUrl: String,
    apiKey: String,
    model: String,
    prompt: String,
    imageBytes: ByteArray?,
    size: String,
    quality: String
): ByteArray {
    require(apiKey.isNotBlank()) { "请填写 API Key" }
    require(prompt.isNotBlank()) { "请填写编辑指令" }
    val sourceImageBytes = requireNotNull(imageBytes) { "无法读取参考图，请重新选择图片后再试" }

    val inputImageDataUrl = buildCompactImageDataUrl(sourceImageBytes)

    val compatPrompt = """
        $prompt

        Reference image is provided in the request image fields. Use it as the visual reference for this edit.
    """.trimIndent()

    val body = JSONObject()
        .put("model", model.trim())
        .put("prompt", compatPrompt)
        .put("n", 1)
        .put("size", size)
        .put("quality", quality)
        .put("image", inputImageDataUrl)
        .put("reference_image", inputImageDataUrl)

    val jsonBody = body.toString()
    val conn = URL(endpoint(baseUrl, "/images/generations")).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 30000
    conn.readTimeout = 180000
    conn.doOutput = true
    conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
    conn.setRequestProperty("Content-Type", "application/json")
    conn.outputStream.use { it.write(jsonBody.toByteArray()) }
    return parseImageResponse(conn)
}
fun callEditResponses(
    baseUrl: String,
    apiKey: String,
    model: String,
    prompt: String,
    imageBytes: ByteArray?,
    size: String,
    quality: String,
    outputFormat: String,
    background: String
): ByteArray {
    require(apiKey.isNotBlank()) { "请填写 API Key" }
    require(prompt.isNotBlank()) { "请填写编辑指令" }
    val sourceImageBytes = requireNotNull(imageBytes) { "无法读取参考图，请重新选择图片后再试" }

    val inputImageDataUrl = "data:image/png;base64," + Base64.encodeToString(sourceImageBytes, Base64.NO_WRAP)

    val inputContent = JSONArray().apply {
        put(
            JSONObject()
                .put("type", "input_text")
                .put("text", "Use the following text as the complete prompt. Do not rewrite it:\n$prompt")
        )
        put(JSONObject().put("type", "input_image").put("image_url", inputImageDataUrl))
    }

    val tool = JSONObject()
        .put("type", "image_generation")
        .put("action", "edit")
        .put("size", size)
        .put("quality", quality)
        .put("output_format", outputFormat)

    if (background.isNotBlank()) {
        tool.put("background", background)
    }

    val body = JSONObject()
        .put("model", model.trim())
        .put(
            "input",
            JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("content", inputContent)
            )
        )
        .put("tools", JSONArray().put(tool))
        .put("tool_choice", "required")

    val conn = URL(endpoint(baseUrl, "/responses")).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 30000
    conn.readTimeout = 180000
    conn.doOutput = true
    conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
    conn.setRequestProperty("Content-Type", "application/json")
    conn.outputStream.use { it.write(body.toString().toByteArray()) }
    return parseResponsesImageResponse(conn, outputFormat)
}
fun parseImageResponse(conn: HttpURLConnection): ByteArray {
    val code = conn.responseCode
    val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
    val text = stream.bufferedReader().use { it.readText() }
    if (code !in 200..299) error("HTTP $code: $text")

    val data = JSONObject(text).optJSONArray("data") ?: error("响应缺少 data")
    val first = data.optJSONObject(0) ?: error("响应 data 为空")
    val b64 = first.optString("b64_json", "")
    if (b64.isNotBlank()) return Base64.decode(b64, Base64.DEFAULT)

    val url = first.optString("url", "")
    if (url.isNotBlank()) return download(url)

    error("响应中既没有 url 也没有 b64_json")
}
fun parseResponsesImageResponse(conn: HttpURLConnection, outputFormat: String): ByteArray {
    val code = conn.responseCode
    val stream = if (code in 200..299) conn.inputStream else conn.errorStream ?: conn.inputStream
    val text = stream.bufferedReader().use { it.readText() }
    if (code !in 200..299) error("HTTP $code: $text")

    val output = JSONObject(text).optJSONArray("output") ?: error("响应缺少 output")
    for (index in 0 until output.length()) {
        val item = output.optJSONObject(index) ?: continue
        if (item.optString("type") != "image_generation_call") continue

        val result = item.optString("result", "")
        if (result.isNotBlank()) {
            val pureBase64 = result.removePrefix("data:image/png;base64,")
                .removePrefix("data:image/jpeg;base64,")
                .removePrefix("data:image/webp;base64,")
            return Base64.decode(pureBase64, Base64.DEFAULT)
        }
    }

    for (index in 0 until output.length()) {
        val item = output.optJSONObject(index) ?: continue
        if (item.optString("type") != "image_generation_call") continue

        val resultUrl = item.optString("url", "")
        if (resultUrl.isNotBlank()) {
            return download(resultUrl)
        }

        val nested = item.optJSONObject("result")
        val nestedUrl = nested?.optString("url", "") ?: ""
        if (nestedUrl.isNotBlank()) {
            return download(nestedUrl)
        }

        val nestedB64 = nested?.optString("b64_json", "") ?: ""
        if (nestedB64.isNotBlank()) {
            return Base64.decode(nestedB64, Base64.DEFAULT)
        }
    }

    error("Responses API 未返回可用图片数据（既没有 result/base64，也没有 url）")
}
fun download(url: String): ByteArray {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 30000
    conn.readTimeout = 180000
    return conn.inputStream.use { it.readBytes() }
}
