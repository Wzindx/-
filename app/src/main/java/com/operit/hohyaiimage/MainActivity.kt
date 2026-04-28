package com.operit.hohyaiimage

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.operit.hohyaiimage.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppTheme { MainScreen() } }
    }
}

data class HistoryItem(
    val time: String,
    val mode: String,
    val model: String,
    val prompt: String,
    val path: String
)

private val generationSizes = listOf("256x256", "512x512", "1024x1024", "1792x1024", "1024x1792")
private val editSizes = listOf("1024x1024", "1536x1024", "1024x1536")
private val qualities = listOf("low", "medium", "high", "auto")
private val outputFormats = listOf("png", "jpeg", "webp")
private val commonModels = listOf("gpt-image-2", "prime/gpt-image-2", "mix/gpt-image-2", "gpt-image-1.5", "dall-e-3")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("config", Context.MODE_PRIVATE) }

    var baseUrl by remember { mutableStateOf(prefs.getString("baseUrl", "https://api.openai.com/v1") ?: "") }
    var apiKey by remember { mutableStateOf(prefs.getString("apiKey", "") ?: "") }
    var model by remember { mutableStateOf(prefs.getString("model", "gpt-image-2") ?: "gpt-image-2") }
    var customModel by remember { mutableStateOf(model) }
    var prompt by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("1024x1024") }
    var quality by remember { mutableStateOf("auto") }
    var count by remember { mutableStateOf("1") }
    var outputFormat by remember { mutableStateOf("png") }
    var background by remember { mutableStateOf("auto") }
    var editMode by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<Uri?>(value = null) }
    var isLoading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("就绪") }
    var imageBytes by remember { mutableStateOf<ByteArray?>(value = null) }
    var history by remember { mutableStateOf(loadHistory(prefs)) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImage = uri
        if (uri != null) editMode = true
    }

    Scaffold(topBar = { TopAppBar(title = { Text("通用图像工坊") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("接口设置", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(baseUrl, { baseUrl = it }, label = { Text("Base URL，例如 https://api.openai.com/v1") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(apiKey, { apiKey = it }, label = { Text("API Key") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(customModel, { customModel = it; model = it }, label = { Text("模型，可自定义") }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            commonModels.take(3).forEach { m ->
                                AssistChip(onClick = { model = m; customModel = m }, label = { Text(m) })
                            }
                        }
                        Button(onClick = {
                            prefs.edit()
                                .putString("baseUrl", baseUrl.trim())
                                .putString("apiKey", apiKey.trim())
                                .putString("model", model.trim())
                                .apply()
                            status = "设置已保存"
                        }) { Text("保存设置") }
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("任务", style = MaterialTheme.typography.titleMedium)
                        Row {
                            FilterChip(selected = !editMode, onClick = { editMode = false }, label = { Text("文生图") })
                            Spacer(Modifier.width(8.dp))
                            FilterChip(selected = editMode, onClick = { editMode = true }, label = { Text("图生图/编辑") })
                        }
                        OutlinedTextField(
                            prompt,
                            { prompt = it },
                            label = { Text(if (editMode) "编辑指令" else "图片描述 Prompt") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SimpleSelect("尺寸", size, if (editMode) editSizes else generationSizes) { size = it }
                            SimpleSelect("质量", quality, qualities) { quality = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                count,
                                { count = it.filter(Char::isDigit).take(2) },
                                label = { Text("数量") },
                                modifier = Modifier.weight(1f)
                            )
                            SimpleSelect("格式", outputFormat, outputFormats) { outputFormat = it }
                        }
                        OutlinedTextField(
                            background,
                            { background = it },
                            label = { Text("background: auto/transparent/opaque") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (editMode) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { picker.launch("image/*") }) { Text("选择参考图") }
                                Text(selectedImage?.lastPathSegment ?: "未选择")
                            }
                        }
                        Button(
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    status = "请求中，生图可能需要 30~180 秒..."
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            if (editMode) {
                                                callEdit(context, baseUrl, apiKey, model, prompt, selectedImage, size, quality, outputFormat, background)
                                            } else {
                                                callGenerate(baseUrl, apiKey, model, prompt, count.toIntOrNull() ?: 1, size, quality)
                                            }
                                        }
                                        imageBytes = result
                                        val saved = saveToGallery(context, result, outputFormat)
                                        val item = HistoryItem(now(), if (editMode) "edit" else "generate", model, prompt, saved)
                                        history = listOf(item) + history.take(29)
                                        saveHistory(prefs, history)
                                        status = "完成，已保存：$saved"
                                    } catch (e: Exception) {
                                        status = "失败：${e.message}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        ) { Text(if (isLoading) "生成中..." else "开始生成") }
                        Text(status)
                    }
                }
            }

            imageBytes?.let { bytes ->
                item {
                    val bitmap = remember(bytes) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    if (bitmap != null) {
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Text("结果预览", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(8.dp))
                                Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("历史记录", style = MaterialTheme.typography.titleMedium)
                        if (history.isEmpty()) Text("暂无历史")
                        history.forEach {
                            Text("${it.time}  ${it.mode}  ${it.model}\n${it.prompt.take(60)}\n${it.path}", style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleSelect(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("$label: $value") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { onChange(it); open = false })
            }
        }
    }
}

fun endpoint(baseUrl: String, path: String): String {
    val b = baseUrl.trim().trimEnd('/')
    return if (b.endsWith("/v1")) "$b$path" else "$b/v1$path"
}

fun callGenerate(baseUrl: String, apiKey: String, model: String, prompt: String, n: Int, size: String, quality: String): ByteArray {
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
    context: Context,
    baseUrl: String,
    apiKey: String,
    model: String,
    prompt: String,
    imageUri: Uri?,
    size: String,
    quality: String,
    outputFormat: String,
    background: String
): ByteArray {
    require(apiKey.isNotBlank()) { "请填写 API Key" }
    require(prompt.isNotBlank()) { "请填写编辑指令" }
    require(imageUri != null) { "请先选择参考图" }

    val boundary = "----AndroidBoundary${UUID.randomUUID()}"
    val conn = URL(endpoint(baseUrl, "/images/edits")).openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.connectTimeout = 30000
    conn.readTimeout = 180000
    conn.doOutput = true
    conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

    conn.outputStream.use { out ->
        fun field(name: String, value: String) {
            out.write("--$boundary\r\n".toByteArray())
            out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray())
        }
        field("model", model.trim())
        field("prompt", prompt)
        field("size", size)
        field("quality", quality)
        field("output_format", outputFormat)
        if (background.isNotBlank()) field("background", background)

        out.write("--$boundary\r\n".toByteArray())
        out.write("Content-Disposition: form-data; name=\"image\"; filename=\"image.png\"\r\n".toByteArray())
        out.write("Content-Type: image/png\r\n\r\n".toByteArray())
        context.contentResolver.openInputStream(imageUri!!)?.use { it.copyTo(out) }
        out.write("\r\n--$boundary--\r\n".toByteArray())
    }
    return parseImageResponse(conn)
}

fun parseImageResponse(conn: HttpURLConnection): ByteArray {
    val code = conn.responseCode
    val stream = if (code in 200..299) {
        conn.inputStream
    } else {
        conn.errorStream ?: conn.inputStream
    }
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

fun download(url: String): ByteArray {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 30000
    conn.readTimeout = 180000
    return conn.inputStream.use { it.readBytes() }
}

fun saveToGallery(context: Context, bytes: ByteArray, format: String): String {
    val ext = if (format == "jpeg") "jpg" else format
    val name = "image_${System.currentTimeMillis()}.$ext"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/$ext")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/UniversalImageStudio")
    }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("无法创建图片文件")
    context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
    return uri.toString()
}

fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

fun loadHistory(prefs: android.content.SharedPreferences): List<HistoryItem> {
    val arr = JSONArray(prefs.getString("history", "[]") ?: "[]")
    return (0 until arr.length()).mapNotNull { i ->
        arr.optJSONObject(i)?.let {
            HistoryItem(
                it.optString("time"),
                it.optString("mode"),
                it.optString("model"),
                it.optString("prompt"),
                it.optString("path")
            )
        }
    }
}

fun saveHistory(prefs: android.content.SharedPreferences, items: List<HistoryItem>) {
    val arr = JSONArray()
    items.forEach {
        arr.put(
            JSONObject()
                .put("time", it.time)
                .put("mode", it.mode)
                .put("model", it.model)
                .put("prompt", it.prompt)
                .put("path", it.path)
        )
    }
    prefs.edit().putString("history", arr.toString()).apply()
}
