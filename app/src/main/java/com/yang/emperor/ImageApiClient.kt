package com.yang.emperor

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.EOFException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

private const val CONNECT_TIMEOUT_MS = 30_000
private const val READ_TIMEOUT_MS = 180_000
private const val MAX_ERROR_BODY_CHARS = 20_000

fun endpoint(baseUrl: String, path: String): String {
    val normalizedBaseUrl = baseUrl.trim().trimEnd('/')
    require(normalizedBaseUrl.isNotBlank()) { "请填写 Base URL" }
    require(
        normalizedBaseUrl.startsWith("https://") || normalizedBaseUrl.startsWith("http://")
    ) { "Base URL 必须以 http:// 或 https:// 开头" }

    val normalizedPath = if (path.startsWith("/")) path else "/$path"
    return if (normalizedBaseUrl.endsWith("/v1")) {
        "$normalizedBaseUrl$normalizedPath"
    } else {
        "$normalizedBaseUrl/v1$normalizedPath"
    }
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
    require(model.isNotBlank()) { "请填写模型 ID" }
    require(prompt.isNotBlank()) { "请填写 Prompt" }

    val body = JSONObject()
        .put("model", model.trim())
        .put("prompt", prompt)
        .put("n", n.coerceIn(1, 10))
        .put("size", size)
        .put("quality", quality)

    val conn = openPostConnection(endpoint(baseUrl, "/images/generations"), apiKey)
    return try {
        writeJsonBody(conn, body)
        parseImageResponse(conn)
    } finally {
        conn.disconnect()
    }
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
    require(model.isNotBlank()) { "请填写模型 ID" }
    require(prompt.isNotBlank()) { "请填写编辑指令" }
    val sourceImageBytes = requireNotNull(imageBytes) { "无法读取参考图，请重新选择图片后再试" }

    val boundary = "----AndroidBoundary${UUID.randomUUID()}"
    val conn = openPostConnection(
        url = endpoint(baseUrl, "/images/edits"),
        apiKey = apiKey,
        contentType = "multipart/form-data; boundary=$boundary"
    )

    return try {
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
        parseImageResponse(conn)
    } finally {
        conn.disconnect()
    }
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
    require(model.isNotBlank()) { "请填写模型 ID" }
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

    val conn = openPostConnection(endpoint(baseUrl, "/images/generations"), apiKey)
    return try {
        writeJsonBody(conn, body)
        parseImageResponse(conn)
    } finally {
        conn.disconnect()
    }
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
    require(model.isNotBlank()) { "请填写模型 ID" }
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

    val conn = openPostConnection(endpoint(baseUrl, "/responses"), apiKey)
    return try {
        writeJsonBody(conn, body)
        parseResponsesImageResponse(conn)
    } finally {
        conn.disconnect()
    }
}

fun parseImageResponse(conn: HttpURLConnection): ByteArray {
    val code = readResponseCode(conn)
    val text = readResponseTextSafely(conn, code)
    if (code !in 200..299) error("HTTP $code: ${text.truncateForError()}")

    val data = JSONObject(text).optJSONArray("data") ?: error("响应缺少 data")
    val first = data.optJSONObject(0) ?: error("响应 data 为空")

    val b64 = first.optString("b64_json", "")
    if (b64.isNotBlank()) return decodeBase64Image(b64)

    val url = first.optString("url", "")
    if (url.isNotBlank()) return download(url)

    error("响应中既没有 url 也没有 b64_json")
}

fun parseResponsesImageResponse(conn: HttpURLConnection): ByteArray {
    val code = readResponseCode(conn)
    val text = readResponseTextSafely(conn, code)
    if (code !in 200..299) error("HTTP $code: ${text.truncateForError()}")

    val output = JSONObject(text).optJSONArray("output") ?: error("响应缺少 output")

    for (index in 0 until output.length()) {
        val item = output.optJSONObject(index) ?: continue
        if (item.optString("type") != "image_generation_call") continue

        val result = item.optString("result", "")
        if (result.isNotBlank()) {
            return decodeBase64Image(result)
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
            return decodeBase64Image(nestedB64)
        }
    }

    error("Responses API 未返回可用图片数据（既没有 result/base64，也没有 url）")
}

fun download(url: String): ByteArray {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = CONNECT_TIMEOUT_MS
    conn.readTimeout = READ_TIMEOUT_MS
    conn.setRequestProperty("Accept", "image/*,*/*")
    conn.setRequestProperty("Connection", "close")

    return try {
        val code = readResponseCode(conn, "图片下载")
        if (code !in 200..299) {
            val errorText = readResponseTextSafely(conn, code).truncateForError()
            error("图片下载失败 HTTP $code: $errorText")
        }
        runCatching {
            conn.inputStream.use { it.readBytes() }
        }.getOrElse { e ->
            throw friendlyNetworkIOException(e, "图片下载响应体读取失败")
        }
    } finally {
        conn.disconnect()
    }
}

private fun openPostConnection(
    url: String,
    apiKey: String,
    contentType: String = "application/json"
): HttpURLConnection {
    return (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = CONNECT_TIMEOUT_MS
        readTimeout = READ_TIMEOUT_MS
        doOutput = true
        setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
        setRequestProperty("Content-Type", contentType)
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Connection", "close")
    }
}

private fun writeJsonBody(conn: HttpURLConnection, body: JSONObject) {
    conn.outputStream.use { out ->
        out.write(body.toString().toByteArray(Charsets.UTF_8))
    }
}

private fun readResponseCode(conn: HttpURLConnection, stage: String = "图像生成接口"): Int {
    return runCatching {
        conn.responseCode
    }.getOrElse { e ->
        throw friendlyNetworkIOException(e, "$stage 读取 HTTP 响应头失败")
    }
}

private fun readResponseTextSafely(conn: HttpURLConnection, code: Int): String {
    return runCatching {
        val stream = if (code in 200..299) {
            conn.inputStream
        } else {
            conn.errorStream ?: conn.inputStream
        }
        stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }.getOrElse { e ->
        throw friendlyNetworkIOException(e, "HTTP $code 响应体读取失败")
    }
}

private fun friendlyNetworkIOException(error: Throwable, stage: String): IOException {
    val chain = generateSequence(error) { it.cause }.toList()
    val allMessages = chain.joinToString("\n") { cause ->
        "${cause.javaClass.name}: ${cause.message.orEmpty()}"
    }
    val isUnexpectedEnd = allMessages.contains("unexpected end of stream", ignoreCase = true) ||
        allMessages.contains("EOFException", ignoreCase = true) ||
        chain.any { it is EOFException }

    val hint = if (isUnexpectedEnd) {
        "网络连接在读取响应时提前断开。常见原因：服务端/代理/网关提前关闭连接、HTTP/1.1 长连接复用异常、接口返回了空响应，或当前网络不稳定。已为请求加入 Connection: close；请重试，或检查 Base URL、代理与服务端稳定性。"
    } else {
        "网络请求失败。请检查网络、Base URL、代理、服务端状态或接口兼容性。"
    }

    val message = buildString {
        append(stage)
        append("\n")
        append(hint)
        append("\n\n原始异常链：\n")
        append(allMessages.ifBlank { "${error.javaClass.name}: ${error.message.orEmpty()}" })
    }

    return IOException(message, error)
}

private fun decodeBase64Image(value: String): ByteArray {
    val pureBase64 = value
        .removePrefix("data:image/png;base64,")
        .removePrefix("data:image/jpeg;base64,")
        .removePrefix("data:image/webp;base64,")
    return Base64.decode(pureBase64, Base64.DEFAULT)
}

private fun String.truncateForError(): String {
    if (length <= MAX_ERROR_BODY_CHARS) return this
    return take(MAX_ERROR_BODY_CHARS) + "\n...（错误响应过长，已截断）"
}
