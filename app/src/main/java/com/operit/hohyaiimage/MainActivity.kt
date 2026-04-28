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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private enum class ScreenRoute {
    MAIN,
    SETTINGS
}

data class HistoryItem(
    val time: String,
    val mode: String,
    val model: String,
    val prompt: String,
    val path: String
)

data class SizeOption(
    val value: String,
    val title: String,
    val desc: String
)

private val imageModels = listOf(
    "gpt-image-2",
    "gpt-image-1",
    "gpt-image-1.5",
    "gpt-image-1-mini",
    "gpt-image-1-high",
    "gpt-image-1-hd",
    "dall-e-3",
    "dall-e-2",
    "prime/gpt-image-2",
    "mix/gpt-image-2"
)

private val generationSizes = listOf(
    SizeOption("1024x1024", "1K 方图", "标准正方形，通用首选"),
    SizeOption("1536x1024", "1.5K 横图", "适合封面、壁纸横构图"),
    SizeOption("1024x1536", "1.5K 竖图", "适合头像、海报竖构图"),
    SizeOption("2048x2048", "2K 方图", "更高细节，更耗时"),
    SizeOption("2048x1152", "2K 横图", "16:9 近似比例，适合横屏"),
    SizeOption("1152x2048", "2K 竖图", "适合竖屏海报"),
    SizeOption("4096x4096", "4K 方图", "超高分辨率，适合精修"),
    SizeOption("4096x2304", "4K 横图", "适合桌面壁纸"),
    SizeOption("2304x4096", "4K 竖图", "适合手机壁纸")
)

private val editSizes = listOf(
    SizeOption("1024x1024", "1K 方图", "编辑稳定、兼容性最好"),
    SizeOption("1536x1024", "1.5K 横图", "横向延展"),
    SizeOption("1024x1536", "1.5K 竖图", "纵向延展"),
    SizeOption("2048x2048", "2K 方图", "高细节编辑")
)

private val qualityOptions = listOf("auto", "low", "medium", "high")
private val outputFormats = listOf("png", "jpeg", "webp")
private val backgroundOptions = listOf("auto", "transparent", "opaque")

private val ratioGuide = listOf(
    "1:1 → 1024x1024 / 2048x2048 / 4096x4096",
    "3:2 → 1536x1024",
    "2:3 → 1024x1536",
    "16:9 → 2048x1152 / 4096x2304",
    "9:16 → 1152x2048 / 2304x4096"
)

private val pageBg = Color(0xFFF4F6FB)
private val cardBg = Color(0xFFF8FAFF)
private val heroStart = Color(0xFF4E67A8)
private val accent = Color(0xFF4B63B3)
private val softAccent = Color(0xFFE8EEFF)
private val successBg = Color(0xFFE9F7EF)
private val successText = Color(0xFF1E7B4D)
private val errorBg = Color(0xFFFFECEC)
private val errorText = Color(0xFFC03B3B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("config", Context.MODE_PRIVATE) }

    var currentRoute by rememberSaveable { mutableStateOf(ScreenRoute.MAIN) }

    var baseUrl by rememberSaveable { mutableStateOf(prefs.getString("baseUrl", "https://api.openai.com/v1") ?: "") }
    var apiKey by rememberSaveable { mutableStateOf(prefs.getString("apiKey", "") ?: "") }
    var model by rememberSaveable { mutableStateOf(prefs.getString("model", "gpt-image-2") ?: "gpt-image-2") }
    var customModel by rememberSaveable { mutableStateOf(model) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var size by rememberSaveable { mutableStateOf("1024x1024") }
    var quality by rememberSaveable { mutableStateOf("auto") }
    var count by rememberSaveable { mutableStateOf("1") }
    var outputFormat by rememberSaveable { mutableStateOf("png") }
    var background by rememberSaveable { mutableStateOf("auto") }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<Uri?>() }
    var isLoading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("欢迎使用，请先在设置页填写接口信息。") }
    var imageBytes by remember { mutableStateOf<ByteArray?>() }
    var history by remember { mutableStateOf(loadHistory(prefs)) }

    val currentSizes = if (editMode) editSizes else generationSizes
    val selectedSizeOption = currentSizes.firstOrNull { it.value == size } ?: currentSizes.first()

    LaunchedEffect(editMode) {
        if (currentSizes.none { it.value == size }) {
            size = currentSizes.first().value
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImage = uri
        if (uri != null) editMode = true
    }

    when (currentRoute) {
        ScreenRoute.SETTINGS -> SettingsScreen(
            baseUrl = baseUrl,
            apiKey = apiKey,
            customModel = customModel,
            currentModel = model,
            recommendedModels = imageModels,
            onBaseUrlChange = { baseUrl = it },
            onApiKeyChange = { apiKey = it },
            onCustomModelChange = {
                customModel = it
                model = it
            },
            onSelectModel = {
                model = it
                customModel = it
            },
            onBack = { currentRoute = ScreenRoute.MAIN },
            onSave = {
                prefs.edit()
                    .putString("baseUrl", baseUrl.trim())
                    .putString("apiKey", apiKey.trim())
                    .putString("model", model.trim())
                    .apply()
                status = "接口设置已保存。"
                currentRoute = ScreenRoute.MAIN
            }
        )

        ScreenRoute.MAIN -> Scaffold(
            containerColor = pageBg,
            topBar = {
                Surface(
                    color = heroStart,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "通用图像工坊",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "首页聚焦任务创建，接口配置移入二级页面，交互更轻量",
                                    color = Color.White.copy(alpha = 0.92f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            TextButton(onClick = { currentRoute = ScreenRoute.SETTINGS }) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(Modifier.size(6.dp))
                                Text("设置", color = Color.White)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SectionTitle("任务模式", "支持文生图与图生图/编辑，常用参数已收敛为下拉选择")
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = !editMode,
                                    onClick = { editMode = false },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                ) {
                                    Text("文生图")
                                }
                                SegmentedButton(
                                    selected = editMode,
                                    onClick = { editMode = true },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                ) {
                                    Text("图生图 / 编辑")
                                }
                            }

                            AppDropdownField(
                                title = "模型",
                                selected = model,
                                options = imageModels,
                                onSelected = {
                                    model = it
                                    customModel = it
                                }
                            )

                            OutlinedTextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                label = { Text(if (editMode) "编辑指令" else "图片描述 Prompt") },
                                placeholder = {
                                    Text(
                                        if (editMode)
                                            "例如：保留主体不变，改成赛博朋克夜景，增强霓虹反射"
                                        else
                                            "例如：一只穿宇航服的橘猫，电影感灯光，超细节"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 140.dp),
                                shape = RoundedCornerShape(20.dp)
                            )

                            if (editMode) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = softAccent),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(onClick = { picker.launch("image/*") }) {
                                            Text("选择参考图")
                                        }
                                        Text(
                                            text = selectedImage?.lastPathSegment ?: "当前未选择图片",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }

                            SectionTitle("尺寸与参数", "尺寸、质量、输出格式、背景与数量全部下拉化，减少首页拥挤与重绘")

                            AppDropdownField(
                                title = "尺寸",
                                selected = selectedSizeOption.title + " · " + selectedSizeOption.value,
                                options = currentSizes.map { "${it.title} · ${it.value}" },
                                onSelected = { display ->
                                    currentSizes.firstOrNull {
                                        "${it.title} · ${it.value}" == display
                                    }?.let { size = it.value }
                                }
                            )

                            InfoCard(
                                title = "当前尺寸",
                                content = "${selectedSizeOption.title} · ${selectedSizeOption.value}\n${selectedSizeOption.desc}"
                            )

                            InfoCard(
                                title = "比例说明",
                                content = ratioGuide.joinToString("\n")
                            )

                            AppDropdownField(
                                title = "质量",
                                selected = quality,
                                options = qualityOptions,
                                onSelected = { quality = it }
                            )

                            AppDropdownField(
                                title = "输出格式",
                                selected = outputFormat,
                                options = outputFormats,
                                onSelected = { outputFormat = it }
                            )

                            AppDropdownField(
                                title = "背景",
                                selected = background,
                                options = backgroundOptions,
                                onSelected = { background = it }
                            )

                            AppDropdownField(
                                title = "生成数量",
                                selected = count,
                                options = (1..10).map { it.toString() },
                                onSelected = { count = it }
                            )

                            if (isLoading) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }

                            Button(
                                enabled = !isLoading,
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        status = "请求已发送，图像生成通常需要 30~180 秒。"
                                        try {
                                            val result = withContext(Dispatchers.IO) {
                                                if (editMode) {
                                                    callEdit(
                                                        context = context,
                                                        baseUrl = baseUrl,
                                                        apiKey = apiKey,
                                                        model = model,
                                                        prompt = prompt,
                                                        imageUri = selectedImage,
                                                        size = size,
                                                        quality = quality,
                                                        outputFormat = outputFormat,
                                                        background = background
                                                    )
                                                } else {
                                                    callGenerate(
                                                        baseUrl = baseUrl,
                                                        apiKey = apiKey,
                                                        model = model,
                                                        prompt = prompt,
                                                        n = count.toIntOrNull() ?: 1,
                                                        size = size,
                                                        quality = quality
                                                    )
                                                }
                                            }
                                            imageBytes = result
                                            val saved = saveToGallery(context, result, outputFormat)
                                            val item = HistoryItem(
                                                time = now(),
                                                mode = if (editMode) "edit" else "generate",
                                                model = model,
                                                prompt = prompt,
                                                path = saved
                                            )
                                            history = listOf(item) + history.take(29)
                                            saveHistory(prefs, history)
                                            status = "生成完成，已保存到相册：$saved"
                                        } catch (e: Exception) {
                                            status = "生成失败：${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isLoading) "正在生成..." else if (editMode) "开始编辑图像" else "开始生成图像")
                            }

                            StatusCard(status)
                        }
                    }
                }

                imageBytes?.let { bytes ->
                    item {
                        val bitmap = remember(bytes) {
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        if (bitmap != null) {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SectionTitle("结果预览", "生成完成后自动显示并写入系统相册")
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color(0xFFD9E1F5), RoundedCornerShape(20.dp))
                                            .padding(8.dp)
                                    ) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SectionTitle("历史记录", "最近 30 条生成 / 编辑历史")
                            if (history.isEmpty()) {
                                Text("暂无历史记录", color = Color.Gray)
                            } else {
                                history.forEachIndexed { index, item ->
                                    HistoryCard(item)
                                    if (index != history.lastIndex) {
                                        HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.45f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    baseUrl: String,
    apiKey: String,
    customModel: String,
    currentModel: String,
    recommendedModels: List<String>,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onCustomModelChange: (String) -> Unit,
    onSelectModel: (String) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        containerColor = pageBg,
        topBar = {
            TopAppBar(
                title = { Text("接口设置") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("返回") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pageBg)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitle("连接配置", "接口信息放入二级页面，不在首页直接裸露")

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = onBaseUrlChange,
                        label = { Text("Base URL") },
                        placeholder = { Text("例如：https://api.openai.com/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text("API Key") },
                        placeholder = { Text("输入你的密钥") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )

                    OutlinedTextField(
                        value = customModel,
                        onValueChange = onCustomModelChange,
                        label = { Text("模型 ID") },
                        placeholder = { Text("支持手动输入兼容站的自定义模型") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    )

                    AppDropdownField(
                        title = "推荐模型",
                        selected = currentModel,
                        options = recommendedModels,
                        onSelected = onSelectModel
                    )

                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存接口设置")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, desc: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
private fun InfoCard(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = accent,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDropdownField(
    title: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(status: String) {
    val isError = status.contains("失败") || status.contains("HTTP")
    val bg = if (isError) errorBg else successBg
    val fg = if (isError) errorText else successText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(fg)
        )
        Text(
            text = status,
            color = fg,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun HistoryCard(item: HistoryItem) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${item.time} · ${item.mode}",
            style = MaterialTheme.typography.labelMedium,
            color = accent
        )
        Text(
            text = item.model,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = item.prompt,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = item.path,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

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
    val sourceImageUri = requireNotNull(imageUri) { "请先选择参考图" }

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
        context.contentResolver.openInputStream(sourceImageUri)?.use { it.copyTo(out) }
        out.write("\r\n--$boundary--\r\n".toByteArray())
    }
    return parseImageResponse(conn)
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

fun now(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

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
