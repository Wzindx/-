package com.yang.emperor

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import com.yang.emperor.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    private val activityTaskScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        activityTaskScope.cancel()
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                AndroidColor.TRANSPARENT,
                AndroidColor.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                AndroidColor.rgb(244, 246, 255),
                AndroidColor.rgb(244, 246, 255)
            )
        )
        setContent { AppTheme { MainScreen(activityTaskScope = activityTaskScope) } }
    }
}

private enum class ScreenRoute {
    MAIN,
    HISTORY,
    SETTINGS
}

private enum class ApiMode(val value: String, val label: String) {
    IMAGES("images", "Images API"),
    RESPONSES("responses", "Responses API"),
    GENERATIONS_EDIT("generations_edit", "Generations 图生图兼容");

    companion object {
        fun from(value: String?): ApiMode =
            entries.firstOrNull { it.value == value } ?: IMAGES
    }
}

data class HistoryItem(
    val time: String,
    val mode: String,
    val model: String,
    val prompt: String,
    val path: String,
    val state: String = "success",
    val error: String = ""
)

private data class ImageTask(
    val id: String,
    val time: String,
    val mode: String,
    val model: String,
    val prompt: String,
    val baseUrl: String,
    val apiKey: String,
    val apiMode: ApiMode,
    val imageBytes: ByteArray?,
    val size: String,
    val quality: String,
    val count: String,
    val outputFormat: String,
    val background: String
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
    "prime/gpt-image-2",
    "mix/gpt-image-2"
)

private val generationSizes = listOf(
    SizeOption("1024x1024", "1:1 方图", "标准正方形，通用首选"),
    SizeOption("1536x1024", "3:2 横图", "适合封面、壁纸横构图"),
    SizeOption("1024x1536", "2:3 竖图", "适合头像、海报竖构图"),
    SizeOption("2048x1536", "4:3 横图", "经典横向比例，适合相机/封面构图"),
    SizeOption("1536x2048", "3:4 竖图", "经典竖向比例，适合人物、头像和手机阅读场景"),
    SizeOption("2048x1152", "16:9 横图", "适合横屏、桌面壁纸、视频封面"),
    SizeOption("1152x2048", "9:16 竖图", "适合手机壁纸、竖屏海报"),
    SizeOption("2048x2048", "1:1 高清方图", "更高细节，更耗时"),
    SizeOption("4096x4096", "1:1 4K 方图", "超高分辨率，适合精修"),
    SizeOption("4096x2304", "16:9 4K 横图", "适合桌面壁纸"),
    SizeOption("2304x4096", "9:16 4K 竖图", "适合手机壁纸")
)

private val editSizes = listOf(
    SizeOption("1024x1024", "1:1 方图", "编辑稳定、兼容性最好"),
    SizeOption("1536x1024", "3:2 横图", "横向延展"),
    SizeOption("1024x1536", "2:3 竖图", "纵向延展"),
    SizeOption("2048x1536", "4:3 横图", "经典横向编辑比例"),
    SizeOption("1536x2048", "3:4 竖图", "经典竖向编辑比例"),
    SizeOption("2048x1152", "16:9 横图", "横屏编辑比例"),
    SizeOption("1152x2048", "9:16 竖图", "竖屏编辑比例"),
    SizeOption("2048x2048", "1:1 高清方图", "高细节编辑")
)

private val qualityOptions = listOf("auto", "low", "medium", "high")
private val outputFormats = listOf("png", "jpeg", "webp")
private val backgroundOptions = listOf("auto", "transparent", "opaque")

private val ratioGuide = listOf(
    "1:1 → 1024x1024 / 2048x2048 / 4096x4096",
    "3:2 → 1536x1024",
    "2:3 → 1024x1536",
    "4:3 → 2048x1536",
    "3:4 → 1536x2048",
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
fun MainScreen(activityTaskScope: CoroutineScope) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { secureConfigPreferences(context) }

    var currentRoute by rememberSaveable { mutableStateOf(ScreenRoute.MAIN) }

    BackHandler(enabled = currentRoute != ScreenRoute.MAIN) {
        currentRoute = ScreenRoute.MAIN
    }

    var baseUrl by rememberSaveable { mutableStateOf(prefs.getString("baseUrl", "https://api.openai.com/v1") ?: "") }
    var apiKey by rememberSaveable { mutableStateOf(prefs.getString("apiKey", "") ?: "") }
    var apiMode by rememberSaveable { mutableStateOf(ApiMode.from(prefs.getString("apiMode", ApiMode.IMAGES.value))) }
    var generateModel by rememberSaveable { mutableStateOf(prefs.getString("generateModel", prefs.getString("model", "gpt-image-2")) ?: "gpt-image-2") }
    var editModel by rememberSaveable { mutableStateOf(prefs.getString("editModel", "gpt-image-2") ?: "gpt-image-2") }
    var customGenerateModel by rememberSaveable { mutableStateOf(generateModel) }
    var customEditModel by rememberSaveable { mutableStateOf(editModel) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var size by rememberSaveable { mutableStateOf("1024x1024") }
    var quality by rememberSaveable { mutableStateOf("auto") }
    var count by rememberSaveable { mutableStateOf("1") }
    var outputFormat by rememberSaveable { mutableStateOf("png") }
    var background by rememberSaveable { mutableStateOf("auto") }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf(null as Uri?) }
    var selectedImageBytes by remember { mutableStateOf(null as ByteArray?) }
    var isReadingReferenceImage by remember { mutableStateOf(false) }
    var showReferenceSheet by rememberSaveable { mutableStateOf(false) }
    var showModelSheet by rememberSaveable { mutableStateOf(false) }
    var showParamsSheet by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var settingsNotice by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf(null as ByteArray?) }
    var history by remember { mutableStateOf(loadHistory(prefs)) }
    var showAdvancedOptions by rememberSaveable { mutableStateOf(false) }
    var showOnboarding by rememberSaveable {
        mutableStateOf(!prefs.getBoolean("onboardingDone", false) && (apiKey.isBlank() || baseUrl.isBlank()))
    }
    val runningTasks = remember { mutableStateListOf<String>() }
    val isConfigured = baseUrl.isNotBlank() && apiKey.isNotBlank()
    val runningCount = runningTasks.size

    val currentSizes = if (editMode) editSizes else generationSizes
    val selectedSizeOption = currentSizes.firstOrNull { it.value == size } ?: currentSizes.first()

    LaunchedEffect(selectedImageBytes) {
        editMode = selectedImageBytes != null
    }

    LaunchedEffect(editMode) {
        if (currentSizes.none { it.value == size }) {
            size = currentSizes.first().value
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImage = uri
        if (uri == null) {
            isReadingReferenceImage = false
            return@rememberLauncherForActivityResult
        }

        isReadingReferenceImage = true
        status = "正在读取参考图..."
        activityTaskScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { readReferenceImageBytes(context, uri) }.getOrNull()
            }

            selectedImageBytes = bytes
            isReadingReferenceImage = false

            if (bytes == null) {
                status = "参考图已选择，但暂时无法读取，请重新选择一次或改用 Images API / Responses API。"
            } else {
                showReferenceSheet = false
                status = ""
            }
        }
    }


    fun startBackgroundTask(task: ImageTask) {
        activityTaskScope.launch {
            runningTasks.add(task.id)
            val runningItem = HistoryItem(
                time = task.time,
                mode = task.mode,
                model = task.model,
                prompt = task.prompt,
                path = "后台处理中",
                state = "running"
            )
            history = listOf(runningItem) + history.take(49)
            saveHistory(prefs, history)
            try {
                val result = withContext(Dispatchers.IO) {
                    if (task.imageBytes != null) {
                        when (task.apiMode) {
                            ApiMode.IMAGES -> callEdit(
                                baseUrl = task.baseUrl,
                                apiKey = task.apiKey,
                                model = task.model,
                                prompt = task.prompt,
                                imageBytes = task.imageBytes,
                                size = task.size,
                                quality = task.quality,
                                outputFormat = task.outputFormat,
                                background = task.background
                            )
                            ApiMode.RESPONSES -> callEditResponses(
                                baseUrl = task.baseUrl,
                                apiKey = task.apiKey,
                                model = task.model,
                                prompt = task.prompt,
                                imageBytes = task.imageBytes,
                                size = task.size,
                                quality = task.quality,
                                outputFormat = task.outputFormat,
                                background = task.background
                            )
                            ApiMode.GENERATIONS_EDIT -> callEditGenerationsCompat(
                                model = task.model,
                                prompt = task.prompt,
                                imageBytes = task.imageBytes,
                                baseUrl = task.baseUrl,
                                apiKey = task.apiKey,
                                size = task.size,
                                quality = task.quality
                            )
                        }
                    } else {
                        callGenerate(
                            baseUrl = task.baseUrl,
                            apiKey = task.apiKey,
                            model = task.model,
                            prompt = task.prompt,
                            n = task.count.toIntOrNull() ?: 1,
                            size = task.size,
                            quality = task.quality
                        )
                    }
                }
                imageBytes = result
                history = history.map {
                    if (it.time == task.time && it.prompt == task.prompt && it.state == "running") {
                        it.copy(path = "未下载", state = "success", error = "")
                    } else it
                }
                saveHistory(prefs, history)
                status = "后台任务完成，可在结果预览中查看或下载。"
            } catch (e: Exception) {
                history = history.map {
                    if (it.time == task.time && it.prompt == task.prompt && it.state == "running") {
                        it.copy(path = "失败", state = "failed", error = e.message ?: "未知错误")
                    } else it
                }
                saveHistory(prefs, history)
                status = "后台任务失败：${e.message}"
            } finally {
                runningTasks.remove(task.id)
            }
            delay(100)
        }
    }

    if (showOnboarding) {
        OnboardingScreen(
            baseUrl = baseUrl,
            apiKey = apiKey,
            onBaseUrlChange = { baseUrl = it },
            onApiKeyChange = { apiKey = it },
            onSkip = {
                prefs.edit().putBoolean("onboardingDone", true).apply()
                showOnboarding = false
            },
            onSave = {
                prefs.edit()
                    .putString("baseUrl", baseUrl.trim())
                    .putString("apiKey", apiKey.trim())
                    .putBoolean("onboardingDone", true)
                    .apply()
                settingsNotice = "接口信息已保存。"
                status = ""
                showOnboarding = false
                currentRoute = ScreenRoute.MAIN
            }
        )
        return
    }

    if (showReferenceSheet) {
        AlertDialog(
            onDismissRequest = { showReferenceSheet = false },
            title = { Text("参考图") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "选择参考图后会自动切换为图生图 / 编辑；清除后自动回到文生图",
                        color = Color(0xFF6B7280)
                    )
                    if (selectedImage != null) {
                        StatusCard(
                            if (selectedImageBytes != null)
                                "当前参考图：${selectedImage?.lastPathSegment ?: "已选择图片"}"
                            else
                                "已记录参考图 URI，但图片缓存读取失败，请重新选择"
                        )
                    } else {
                        Text("当前未选择参考图，将使用文生图模式。", color = Color(0xFF6B7280))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { picker.launch("image/*") }) {
                    Text(if (selectedImage == null) "选择参考图" else "更换参考图")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = selectedImage != null || selectedImageBytes != null,
                    onClick = {
                        selectedImage = null
                        selectedImageBytes = null
                        showReferenceSheet = false
                        status = "已清除参考图，将自动使用文生图模式。"
                    }
                ) {
                    Text("清除参考图")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showModelSheet) {
        AppBottomSheetPanel(
            title = "接口与模型",
            description = "修改后会自动保存。",
            onDismiss = { showModelSheet = false }
        ) {
            AppDropdownField(
                title = "接口模式",
                selected = apiMode.label,
                options = ApiMode.entries.map { it.label },
                onSelected = { label ->
                    ApiMode.entries.firstOrNull { it.label == label }?.let { apiMode = it }
                }
            )
            AppEditableDropdownField(
                title = "文生图模型 ID",
                value = customGenerateModel,
                options = imageModels,
                placeholder = "可手动输入，也可从推荐模型中选择",
                onValueChange = {
                    customGenerateModel = it
                    generateModel = it
                },
                onSelected = {
                    customGenerateModel = it
                    generateModel = it
                }
            )
            AppEditableDropdownField(
                title = "图生图模型 ID",
                value = customEditModel,
                options = imageModels,
                placeholder = "可手动输入，也可从推荐模型中选择",
                onValueChange = {
                    customEditModel = it
                    editModel = it
                },
                onSelected = {
                    customEditModel = it
                    editModel = it
                }
            )
            Button(
                onClick = {
                    prefs.edit()
                        .putString("apiMode", apiMode.value)
                        .putString("generateModel", generateModel.trim())
                        .putString("editModel", editModel.trim())
                        .putString("model", generateModel.trim())
                        .apply()
                    status = "接口与模型已保存。"
                    showModelSheet = false
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成")
            }
        }
    }

    if (showParamsSheet) {
        AppBottomSheetPanel(
            title = "生成参数",
            description = "尺寸、画质和输出格式会用于下一次生成。",
            onDismiss = { showParamsSheet = false }
        ) {
            AppDropdownField(
                title = "尺寸 / 比例",
                selected = selectedSizeOption.title + " · " + selectedSizeOption.value,
                options = currentSizes.map { "${it.title} · ${it.value}" },
                onSelected = { display ->
                    currentSizes.firstOrNull {
                        "${it.title} · ${it.value}" == display
                    }?.let { size = it.value }
                }
            )
            AppDropdownField(
                title = "画质",
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
            Button(
                onClick = { showParamsSheet = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成")
            }
        }
    }

    when (currentRoute) {
        ScreenRoute.SETTINGS -> Scaffold(
            containerColor = pageBg,
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onRouteSelected = { currentRoute = it }
                )
            }
        ) { settingsPadding ->
            SettingsScreen(
            baseUrl = baseUrl,
            apiKey = apiKey,
            apiMode = apiMode,
            customGenerateModel = customGenerateModel,
            currentGenerateModel = generateModel,
            customEditModel = customEditModel,
            currentEditModel = editModel,
            recommendedModels = imageModels,
            onBaseUrlChange = { baseUrl = it },
            onApiKeyChange = { apiKey = it },
            onApiModeChange = { apiMode = it },
            onCustomGenerateModelChange = {
                customGenerateModel = it
                generateModel = it
            },
            onSelectGenerateModel = {
                generateModel = it
                customGenerateModel = it
            },
            onCustomEditModelChange = {
                customEditModel = it
                editModel = it
            },
            onSelectEditModel = {
                editModel = it
                customEditModel = it
            },
            settingsNotice = settingsNotice,
            onBack = { currentRoute = ScreenRoute.MAIN },
            onClearConfig = {
                prefs.edit().clear().apply()
                baseUrl = "https://api.openai.com/v1"
                apiKey = ""
                apiMode = ApiMode.IMAGES
                generateModel = "gpt-image-2"
                editModel = "gpt-image-2"
                customGenerateModel = generateModel
                customEditModel = editModel
                history = emptyList()
                settingsNotice = "已清除接口配置、密钥和历史记录，请重新填写接口设置。"
                currentRoute = ScreenRoute.SETTINGS
            },
            onShowOnboarding = {
                showOnboarding = true
            },
            onSave = {
                prefs.edit()
                    .putString("baseUrl", baseUrl.trim())
                    .putString("apiKey", apiKey.trim())
                    .putString("apiMode", apiMode.value)
                    .putString("generateModel", generateModel.trim())
                    .putString("editModel", editModel.trim())
                    .putString("model", generateModel.trim())
                    .putBoolean("onboardingDone", true)
                    .apply()
                settingsNotice = "接口设置已保存。"
                status = ""
                currentRoute = ScreenRoute.MAIN
            },
            outerPadding = settingsPadding
        )
        }

        ScreenRoute.MAIN -> Scaffold(
            containerColor = pageBg,
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onRouteSelected = { currentRoute = it }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBg)
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "通用图像工坊",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "输入提示词，选图后会自动切换为图生图。",
                            color = Color(0xFF6B7280),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF5F6FF)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            SectionTitle("创作", "")
                            if (status.isNotBlank()) {
                                StatusCard(status)
                            }

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

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable(enabled = !isReadingReferenceImage) { picker.launch("image/*") },
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = RoundedCornerShape(20.dp),
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = if (isReadingReferenceImage) "读取图片中..." else if (selectedImageBytes != null) "更换图片" else "选择图片",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (isReadingReferenceImage) {
                                            Text(
                                                text = "正在读取参考图，请稍候",
                                                color = Color(0xFF6B7280),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        } else if (selectedImageBytes != null) {
                                            Text(
                                                text = selectedImage?.lastPathSegment ?: "已选择图片",
                                                color = Color(0xFF6B7280),
                                                style = MaterialTheme.typography.labelMedium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = "›",
                                        fontSize = 30.sp,
                                        color = accent,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (runningCount > 0) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                Text(
                                    text = "后台处理中：${runningCount} 个",
                                    color = Color(0xFF6B7280),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Button(
                                    enabled = prompt.isNotBlank() && isConfigured,
                                    onClick = {
                                        val task = ImageTask(
                                            id = UUID.randomUUID().toString(),
                                            time = now(),
                                            mode = if (selectedImageBytes != null) "edit" else "generate",
                                            model = if (selectedImageBytes != null) editModel else generateModel,
                                            prompt = prompt,
                                            baseUrl = baseUrl.trim(),
                                            apiKey = apiKey.trim(),
                                            apiMode = apiMode,
                                            imageBytes = selectedImageBytes,
                                            size = size,
                                            quality = quality,
                                            count = count,
                                            outputFormat = outputFormat,
                                            background = background
                                        )
                                        startBackgroundTask(task)
                                        status = "已开始后台生成，可继续创建新任务。"
                                        currentRoute = ScreenRoute.HISTORY
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )) {
                                    Text("生成图像")
                                }

                            ConfigEntryCard(
                                title = "接口与模型",
                                primary = apiMode.label,
                                secondary = "模型：${if (selectedImageBytes != null) editModel else generateModel}",
                                onClick = { showModelSheet = true }
                            )

                            ConfigEntryCard(
                                title = "生成参数",
                                primary = selectedSizeOption.title + " · " + selectedSizeOption.value,
                                secondary = "画质 $quality · $outputFormat · 数量 $count",
                                onClick = { showParamsSheet = true }
                            )
                        }
                    }
                }

                imageBytes?.let { bytes ->
                    item {
                        val bitmap = remember(bytes) {
                            decodePreviewBitmap(bytes)
                        }
                        if (bitmap != null) {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    SectionTitle("结果预览", "生成完成后可查看或手动下载到相册")
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
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        TextButton(
                                            onClick = {
                                                status = "结果已显示在预览区。"
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("查看")
                                        }
                                        Button(
                                            onClick = {
                                                val saved = saveToGallery(context, bytes, outputFormat)
                                                status = "已下载到相册：$saved"
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("下载")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }

        ScreenRoute.HISTORY -> Scaffold(
            containerColor = pageBg,
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onRouteSelected = { currentRoute = it }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBg)
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("历史记录", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("最近生成与编辑结果", color = Color(0xFF6B7280))
                        }
                        TextButton(onClick = {
                            history = emptyList()
                            saveHistory(prefs, history)
                            status = "历史记录已清空。"
                        }) {
                            Text("清空")
                        }
                    }
                }
                item {
                    HistoryStatsCard(
                        successCount = history.count { it.state == "success" },
                        failedCount = history.count { it.state == "failed" },
                        runningCount = history.count { it.state == "running" }
                    )
                }

                if (history.isEmpty()) {
                    item {
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp, horizontal = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "暂无历史记录",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                } else {
                    items(history.take(30)) { item ->
                        HistoryCard(item)
                    }
                }
            }
        }
    }
}


@Composable
private fun OnboardingScreen(
    baseUrl: String,
    apiKey: String,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onSkip: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(containerColor = pageBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("欢迎使用通用图像工坊", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("首次使用需要先填写接口地址和 API Key。以后也可以在设置页重新打开引导。", color = Color(0xFF6B7280))
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
                    Button(
                        onClick = onSave,
                        enabled = baseUrl.isNotBlank() && apiKey.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存并开始使用")
                    }
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("暂不设置，进入应用")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBottomSheetPanel(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = Dp.Unspecified,
        containerColor = Color(0xFFF4F6FF),
        contentColor = Color(0xFF111827),
        scrimColor = Color.Transparent,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        tonalElevation = 8.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 64.dp, height = 7.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color(0xFF9CA3AF).copy(alpha = 0.7f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF4F6FF))
                .heightIn(max = 560.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )
            Text(
                text = description,
                color = Color(0xFF6B7280),
                style = MaterialTheme.typography.bodyMedium
            )
            content()
        }
    }
}

@Composable
private fun HistoryStatsCard(
    successCount: Int,
    failedCount: Int,
    runningCount: Int
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF5F6FF)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HistoryStatItem(successCount, "处理成功", Color(0xFF16A34A))
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .border(0.5.dp, Color(0xFFD1D5DB))
            )
            HistoryStatItem(failedCount, "处理失败", Color(0xFFDC2626))
            Box(
                modifier = Modifier
                    .height(42.dp)
                    .border(0.5.dp, Color(0xFFD1D5DB))
            )
            HistoryStatItem(runningCount, "处理中", Color(0xFFD97706))
        }
    }
}

@Composable
private fun HistoryStatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            color = color,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BottomNavigationBar(
    currentRoute: ScreenRoute,
    onRouteSelected: (ScreenRoute) -> Unit
) {
    Surface(
        color = Color(0xFFF7F8FC),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BottomNavButton(
                text = "创作",
                selected = currentRoute == ScreenRoute.MAIN,
                modifier = Modifier.weight(1f),
                onClick = { onRouteSelected(ScreenRoute.MAIN) }
            )
            BottomNavButton(
                text = "历史",
                selected = currentRoute == ScreenRoute.HISTORY,
                modifier = Modifier.weight(1f),
                onClick = { onRouteSelected(ScreenRoute.HISTORY) }
            )
            BottomNavButton(
                text = "设置",
                selected = currentRoute == ScreenRoute.SETTINGS,
                modifier = Modifier.weight(1f),
                onClick = { onRouteSelected(ScreenRoute.SETTINGS) }
            )
        }
    }
}

@Composable
private fun BottomNavButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val content = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
    TextButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
    ) {
        Text(text, color = content, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    baseUrl: String,
    apiKey: String,
    apiMode: ApiMode,
    customGenerateModel: String,
    currentGenerateModel: String,
    customEditModel: String,
    currentEditModel: String,
    recommendedModels: List<String>,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onApiModeChange: (ApiMode) -> Unit,
    onCustomGenerateModelChange: (String) -> Unit,
    onSelectGenerateModel: (String) -> Unit,
    onCustomEditModelChange: (String) -> Unit,
    onSelectEditModel: (String) -> Unit,
    settingsNotice: String,
    onBack: () -> Unit,
    onClearConfig: () -> Unit,
    onShowOnboarding: () -> Unit,
    onSave: () -> Unit,
    outerPadding: PaddingValues = PaddingValues()
) {
    Scaffold(
        containerColor = pageBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(outerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitle("连接配置", "这里只保留接口地址和密钥；模型与生成参数在首页二级栏调整")

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


                    Button(
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("保存连接设置")
                    }

                    TextButton(
                        onClick = onShowOnboarding,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("再来一次引导")
                    }

                    TextButton(
                        onClick = onClearConfig,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清除密钥、配置和历史记录")
                    }

                    if (settingsNotice.isNotBlank()) {
                        StatusCard(settingsNotice)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigEntryCard(
    title: String,
    primary: String,
    secondary: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = primary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF6B7280),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "›",
                fontSize = 34.sp,
                color = accent,
                fontWeight = FontWeight.SemiBold
            )
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
        if (desc.isNotBlank()) {
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
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
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selected,
                onValueChange = {},
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                )
            )
            // 透明点击层覆盖在 disabled TextField 上
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = !expanded }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(1.dp)
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(x = 0.dp, y = 8.dp),
                    modifier = Modifier
                        .width(280.dp)
                        .heightIn(max = 220.dp)
                        .background(MaterialTheme.colorScheme.surface)
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
}

@Composable
private fun AppEditableDropdownField(
    title: String,
    value: String,
    options: List<String>,
    placeholder: String,
    onValueChange: (String) -> Unit,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var customInput by remember { mutableStateOf("") }

    // 自定义输入弹窗
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("自定义模型") },
            text = {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    placeholder = { Text("输入模型名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (customInput.isNotBlank()) {
                            onValueChange(customInput)
                            onSelected(customInput)
                        }
                        showCustomDialog = false
                        customInput = ""
                    }
                ) { Text("确定") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomDialog = false
                        customInput = ""
                    }
                ) { Text("取消") }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                placeholder = { Text(placeholder) },
                singleLine = true,
                readOnly = true,
                enabled = false,
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                )
            )
            // 透明点击层
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { expanded = !expanded }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(1.dp)
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = DpOffset(x = 0.dp, y = 8.dp),
                    modifier = Modifier
                        .width(280.dp)
                        .heightIn(max = 220.dp)
                        .background(MaterialTheme.colorScheme.surface)
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
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "自定义输入",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        onClick = {
                            expanded = false
                            showCustomDialog = true
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
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF5F6FF)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = item.model,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val pill = when (item.state) {
                        "running" -> Triple("处理中", Color(0xFFFFF3D6), Color(0xFFD97706))
                        "failed" -> Triple("处理失败", Color(0xFFFFE4E6), Color(0xFFE11D48))
                        else -> Triple("处理成功", Color(0xFFDFFBEA), Color(0xFF15803D))
                    }
                    StatusPill(
                        text = pill.first,
                        bg = pill.second,
                        fg = pill.third
                    )
                }
                Text(
                    text = if (item.error.isNotBlank()) "错误：${item.error}" else "图片信息：${item.path}",
                    color = Color(0xFF4B5563),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${item.time} · ${if (item.mode == "edit") "图生图" else "文生图"}",
                    color = Color(0xFF4B5563),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(
                onClick = {},
                modifier = Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFEDE9FE))
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "查看",
                    color = Color(0xFF4C1D95),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    bg: Color,
    fg: Color
) {
    Text(
        text = text,
        color = fg,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

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

fun endpoint(baseUrl: String, path: String): String {
    val b = baseUrl.trim().trimEnd('/')
    return if (b.endsWith("/v1")) "$b$path" else "$b/v1$path"
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
        fun field(name: String, value: String) {
            out.write("--$boundary\\r\\n".toByteArray())
            out.write("Content-Disposition: form-data; name=\"$name\"\\r\\n\\r\\n$value\\r\\n".toByteArray())
        }

        field("model", model.trim())
        field("prompt", prompt)
        field("size", size)
        field("quality", quality)
        field("output_format", outputFormat)
        if (background.isNotBlank()) field("background", background)

        out.write("--$boundary\\r\\n".toByteArray())
        out.write("Content-Disposition: form-data; name=\"image\"; filename=\"image.png\"\\r\\n".toByteArray())
        out.write("Content-Type: image/png\\r\\n\\r\\n".toByteArray())
        out.write(sourceImageBytes)
        out.write("\\r\\n--$boundary--\\r\\n".toByteArray())
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
        put(JSONObject().put("type", "input_text").put("text", "Use the following text as the complete prompt. Do not rewrite it:\\n$prompt"))
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


fun secureConfigPreferences(context: Context): android.content.SharedPreferences {
    val legacyPrefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_config",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    if (!securePrefs.getBoolean("secureMigratedFromV16", false)) {
        val editor = securePrefs.edit()
        val keys = listOf("baseUrl", "apiKey", "apiMode", "generateModel", "editModel", "model", "history")
        keys.forEach { key ->
            val value = legacyPrefs.getString(key, null)
            if (value != null) editor.putString(key, value)
        }
        editor.putBoolean("secureMigratedFromV16", true).apply()

        legacyPrefs.edit()
            .remove("baseUrl")
            .remove("apiKey")
            .remove("apiMode")
            .remove("generateModel")
            .remove("editModel")
            .remove("model")
            .remove("history")
            .apply()
    }

    return securePrefs
}

fun loadHistory(prefs: android.content.SharedPreferences): List<HistoryItem> {
    val raw = prefs.getString("history", "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let {
                HistoryItem(
                    time = it.optString("time"),
                    mode = it.optString("mode"),
                    model = it.optString("model"),
                    prompt = it.optString("prompt"),
                    path = it.optString("path"),
                    state = it.optString("state", "success"),
                    error = it.optString("error", "")
                )
            }
        }
    }.getOrElse { emptyList() }
}

fun saveHistory(prefs: android.content.SharedPreferences, items: List<HistoryItem>) {
    val arr = JSONArray()
    items.take(50).forEach {
        arr.put(
            JSONObject()
                .put("time", it.time)
                .put("mode", it.mode)
                .put("model", it.model)
                .put("prompt", it.prompt)
                .put("path", it.path)
                .put("state", it.state)
                .put("error", it.error)
        )
    }
    prefs.edit().putString("history", arr.toString()).apply()
}
