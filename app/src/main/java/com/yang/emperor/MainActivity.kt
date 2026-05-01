package com.yang.emperor

import android.Manifest
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalOverscrollConfiguration
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.produceState
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
import java.util.UUID
import java.net.URL
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

private const val DEVELOPER_QQ = "2753761311"
private const val DEVELOPER_QQ_AVATAR_URL = "https://q1.qlogo.cn/g?b=qq&nk=$DEVELOPER_QQ&s=640"
private const val IMAGEFORGE_REPO_URL = "https://github.com/Wzindx/ImageForge"
private const val IMAGEFORGE_VERSION_NAME = "2.1"

private suspend fun loadDeveloperAvatarBitmap(): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL(DEVELOPER_QQ_AVATAR_URL).openConnection()
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 ImageForge")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
        connection.getInputStream().use { input ->
            BitmapFactory.decodeStream(input)
        }
    }.getOrNull()
}

private fun openDeveloperQQ(context: Context) {
    val qqCardIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("mqqapi://card/show_pslcard?src_type=internal&version=1&uin=$DEVELOPER_QQ&card_type=person&source=qrcode")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val qqCardFallbackIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("mqq://card/show_pslcard?src_type=internal&version=1&uin=$DEVELOPER_QQ&card_type=person&source=qrcode")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://qm.qq.com/cgi-bin/qm/qr?k=&jump_from=webapi&authKey=&uin=$DEVELOPER_QQ")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    runCatching {
        context.startActivity(qqCardIntent)
    }.getOrElse {
        runCatching {
            context.startActivity(qqCardFallbackIntent)
        }.getOrElse {
            runCatching { context.startActivity(webIntent) }
        }
    }
}

private fun openImageForgeRepository(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(IMAGEFORGE_REPO_URL)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
}


private object ImageForgeBackgroundRunner {
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
}

private fun compactErrorMessage(message: String): String {
    return message.lines().firstOrNull { it.isNotBlank() }?.take(140) ?: "未知错误"
}

private fun detailedTaskErrorMessage(e: Exception, task: ImageTask): String {
    val raw = e.stackTraceToString()
    val root = generateSequence(e as Throwable?) { it.cause }.lastOrNull()
    val rootName = root?.javaClass?.simpleName ?: e.javaClass.simpleName
    val rootMessage = root?.message ?: e.message ?: "无详细异常消息"

    val reason = when (root) {
        is SocketTimeoutException -> "请求超时：接口在限定时间内没有返回结果。"
        is UnknownHostException -> "网络解析失败：无法解析 Base URL 的域名。"
        is IOException -> "网络请求失败：连接被中断、服务端无响应或当前网络不可用。"
        else -> when {
            rootMessage.contains("timeout", ignoreCase = true) -> "请求超时：模型生成耗时较长或服务端排队。"
            rootMessage.contains("401") -> "认证失败：API Key 可能无效或权限不足。"
            rootMessage.contains("403") -> "请求被拒绝：账号、模型或接口权限可能不足。"
            rootMessage.contains("404") -> "接口不存在：Base URL、接口模式或模型路径可能不匹配。"
            rootMessage.contains("429") -> "请求过多：服务端限流或额度不足。"
            rootMessage.contains("500") || rootMessage.contains("502") || rootMessage.contains("503") -> "服务端错误：图像服务暂时不可用。"
            else -> "生成失败：接口返回异常或请求处理失败。"
        }
    }

    return buildString {
        appendLine(reason)
        appendLine()
        appendLine("任务信息：")
        appendLine("- 模式：${if (task.mode == "edit") "图生图 / 编辑" else "文生图"}")
        appendLine("- 模型：${task.model}")
        appendLine("- 接口模式：${task.apiMode.label}")
        appendLine("- 尺寸：${task.size}")
        appendLine("- 画质：${task.quality}")
        appendLine("- Base URL：${task.baseUrl}")
        appendLine()
        appendLine("建议处理：")
        appendLine("1. 检查网络或代理是否稳定。")
        appendLine("2. 如果是 timeout，可稍后重试或换响应更快的模型。")
        appendLine("3. 检查 Base URL 是否与当前接口模式匹配。")
        appendLine("4. 检查 API Key、余额、模型权限和服务端限流。")
        appendLine()
        appendLine("原始异常：")
        appendLine("$rootName: $rootMessage")
        appendLine()
        appendLine("堆栈摘要：")
        append(raw.take(1800))
    }.trim()
}


class MainActivity : ComponentActivity() {
    private val activityTaskScope = ImageForgeBackgroundRunner.scope

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(activityTaskScope: CoroutineScope) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { secureConfigPreferences(context) }

    var currentRoute by rememberSaveable { mutableStateOf(ScreenRoute.MAIN) }

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
    var showReferenceSheet by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showParamsSheet by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var settingsNotice by remember { mutableStateOf("") }
    var imageBytes by remember { mutableStateOf(null as ByteArray?) }
    var previewPrompt by remember { mutableStateOf("") }
    var previewSavedPath by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(loadHistory(prefs)) }
    val selectedHistoryKeys = remember { mutableStateListOf<String>() }
    var showAdvancedOptions by rememberSaveable { mutableStateOf(false) }
    val shouldShowInitialOnboarding = remember {
        !prefs.getBoolean("onboardingDone", false) && (apiKey.isBlank() || baseUrl.isBlank())
    }
    var showOnboarding by remember { mutableStateOf(shouldShowInitialOnboarding) }
    var onboardingReturnRoute by remember { mutableStateOf(ScreenRoute.MAIN.name) }
    var onboardingSessionId by remember { mutableLongStateOf(0L) }
    val runningTasks = remember { mutableStateListOf<String>() }

    BackHandler(enabled = currentRoute != ScreenRoute.MAIN && !showOnboarding) {
        currentRoute = ScreenRoute.MAIN
    }

    val isConfigured = baseUrl.isNotBlank() && apiKey.isNotBlank()
    val runningCount = runningTasks.size

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    val currentSizes = if (editMode) editSizes else generationSizes
    val selectedSizeOption = currentSizes.firstOrNull { it.value == size } ?: currentSizes.first()

    LaunchedEffect(selectedImage) {
        editMode = selectedImage != null
    }

    LaunchedEffect(editMode) {
        if (currentSizes.none { it.value == size }) {
            size = currentSizes.first().value
        }
    }

    LaunchedEffect(settingsNotice) {
        if (settingsNotice.isNotBlank()) {
            delay(5000)
            settingsNotice = ""
        }
    }

    LaunchedEffect(status) {
        if (status.isNotBlank()) {
            delay(5000)
            status = ""
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImage = uri
        selectedImageBytes = null
        isReadingReferenceImage = false

        if (uri == null) {
            status = ""
            return@rememberLauncherForActivityResult
        }

        showReferenceSheet = false
        status = "参考图已选择，将在生成时读取，避免打开图片后卡顿。"
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
                previewPrompt = task.prompt
                val savedUri = runCatching {
                    saveToGallery(context, result, task.outputFormat)
                }.getOrElse { "未下载" }
                previewSavedPath = savedUri

                history = history.map {
                    if (it.time == task.time && it.prompt == task.prompt && it.state == "running") {
                        it.copy(path = savedUri, state = "success", error = "")
                    } else it
                }
                saveHistory(prefs, history)
                status = if (savedUri.startsWith("content://")) {
                    "后台任务完成，已保存到相册。"
                } else {
                    "后台任务完成，可在结果预览中查看或下载。"
                }
                notifyImageReady(context, savedUri)
            } catch (e: Exception) {
                val detailedError = detailedTaskErrorMessage(e, task)
                history = history.map {
                    if (it.time == task.time && it.prompt == task.prompt && it.state == "running") {
                        it.copy(path = "失败", state = "failed", error = detailedError)
                    } else it
                }
                saveHistory(prefs, history)
                status = "后台任务失败：${compactErrorMessage(detailedError)}"
            } finally {
                runningTasks.remove(task.id)
            }
            delay(100)
        }
    }

    if (showOnboarding) {
        key(onboardingSessionId) {
            OnboardingScreen(
                baseUrl = baseUrl,
                apiKey = apiKey,
                onBaseUrlChange = { baseUrl = it },
                onApiKeyChange = { apiKey = it },
                onSkip = {
                    prefs.edit { putBoolean("onboardingDone", true) }
                    showOnboarding = false
                    currentRoute = runCatching { ScreenRoute.valueOf(onboardingReturnRoute) }.getOrDefault(ScreenRoute.MAIN)
                },
                onSave = {
                    prefs.edit {
                        putString("baseUrl", baseUrl.trim())
                        putString("apiKey", apiKey.trim())
                        putBoolean("onboardingDone", true)
                    }
                    settingsNotice = "接口信息已保存。"
                    status = ""
                    showOnboarding = false
                    currentRoute = runCatching { ScreenRoute.valueOf(onboardingReturnRoute) }.getOrDefault(ScreenRoute.MAIN)
                }
            )
        }
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
                    prefs.edit {
                        putString("apiMode", apiMode.value)
                        putString("generateModel", generateModel.trim())
                        putString("editModel", editModel.trim())
                        putString("model", generateModel.trim())
                    }
                    status = "接口与模型已保存，将用于下一次生成。"
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
                prefs.edit { clear() }
                baseUrl = "https://api.openai.com/v1"
                apiKey = ""
                apiMode = ApiMode.IMAGES
                generateModel = "gpt-image-2"
                editModel = "gpt-image-2"
                customGenerateModel = generateModel
                customEditModel = editModel
                history = emptyList()
                settingsNotice = "已清除接口配置、密钥和图片记录，请重新填写接口设置。"
                currentRoute = ScreenRoute.SETTINGS
            },
            onShowOnboarding = {
                currentRoute = ScreenRoute.SETTINGS
                onboardingReturnRoute = ScreenRoute.SETTINGS.name
                onboardingSessionId = System.nanoTime()
                showOnboarding = true
            },
            onSave = {
                prefs.edit {
                    putString("baseUrl", baseUrl.trim())
                    putString("apiKey", apiKey.trim())
                    putString("apiMode", apiMode.value)
                    putString("generateModel", generateModel.trim())
                    putString("editModel", editModel.trim())
                    putString("model", generateModel.trim())
                    putBoolean("onboardingDone", true)
                }
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
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
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
                                            text = if (selectedImage != null) "更换图片" else "选择图片",
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
                                        } else if (selectedImage != null) {
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
                                enabled = prompt.isNotBlank() && isConfigured && !isReadingReferenceImage,
                                onClick = {
                                    ensureImageNotificationChannel(context)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }

                                    activityTaskScope.launch {
                                        val referenceBytes = selectedImage?.let { uri ->
                                            isReadingReferenceImage = true
                                            status = "正在读取参考图..."
                                            withContext(Dispatchers.IO) {
                                                runCatching { readReferenceImageBytes(context, uri) }
                                            }.onFailure {
                                                status = "参考图读取失败：${it.message ?: "未知错误"}"
                                            }.getOrNull().also {
                                                selectedImageBytes = it
                                                isReadingReferenceImage = false
                                            }
                                        }

                                        if (selectedImage != null && referenceBytes == null) {
                                            return@launch
                                        }

                                        val task = ImageTask(
                                            id = UUID.randomUUID().toString(),
                                            time = now(),
                                            mode = if (referenceBytes != null) "edit" else "generate",
                                            model = if (referenceBytes != null) editModel else generateModel,
                                            prompt = prompt,
                                            baseUrl = baseUrl.trim(),
                                            apiKey = apiKey.trim(),
                                            apiMode = apiMode,
                                            imageBytes = referenceBytes,
                                            size = size,
                                            quality = quality,
                                            count = count,
                                            outputFormat = outputFormat,
                                            background = background
                                        )
                                        startBackgroundTask(task)
                                        status = "已提交后台生成任务，可继续创建新任务。"
                                        currentRoute = ScreenRoute.HISTORY
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(if (isReadingReferenceImage) "读取参考图..." else "生成图像")
                            }

                            ConfigEntryCard(
                                title = "接口与模型",
                                primary = apiMode.label,
                                secondary = "模型：${if (selectedImage != null) editModel else generateModel}",
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
                                    SectionTitle("结果预览", "图片、提示词、保存、分享和关闭都集中在这里")
                                    StatusCard("默认保存路径：系统相册 / Pictures/ImageForge")
                                    if (previewPrompt.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            shape = RoundedCornerShape(16.dp),
                                            tonalElevation = 0.dp
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(14.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text("提示词", fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = previewPrompt,
                                                    color = Color(0xFF4B5563),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                TextButton(onClick = {
                                                    copyTextToClipboard(context, "ImageForge Prompt", previewPrompt)
                                                    status = "提示词已复制。"
                                                }) {
                                                    Text("复制提示词")
                                                }
                                            }
                                        }
                                    }
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val saved = saveToGallery(context, bytes, outputFormat)
                                                previewSavedPath = saved
                                                status = "已保存到相册：$saved"
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("保存")
                                        }
                                        TextButton(
                                            onClick = {
                                                val pathForShare = if (previewSavedPath.startsWith("content://")) {
                                                    previewSavedPath
                                                } else {
                                                    saveToGallery(context, bytes, outputFormat).also { previewSavedPath = it }
                                                }
                                                shareImageFromHistory(context, pathForShare)
                                                status = "已打开系统分享。"
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("分享")
                                        }
                                        TextButton(
                                            onClick = {
                                                imageBytes = null
                                                previewPrompt = ""
                                                previewSavedPath = ""
                                                status = "已关闭结果预览。"
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("关闭")
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

        ScreenRoute.HISTORY -> Scaffold(
            containerColor = pageBg,
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onRouteSelected = { currentRoute = it }
                )
            }
        ) { padding ->
            CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(pageBg)
                    .padding(padding)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 80.dp),
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
                            Text("图片记录", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("最近生成与编辑的图片", color = Color(0xFF6B7280))
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
                                    "暂无图片记录",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                } else {
                    items(history.take(30)) { item ->
                        val itemKey = "${item.time}|${item.prompt}"
                        HistoryCard(
                            item = item,
                            selectionMode = selectedHistoryKeys.isNotEmpty(),
                            selected = itemKey in selectedHistoryKeys,
                            onToggleSelected = {
                                if (itemKey in selectedHistoryKeys) {
                                    selectedHistoryKeys.remove(itemKey)
                                } else {
                                    selectedHistoryKeys.add(itemKey)
                                }
                            },
                            onLongPress = {
                                if (itemKey !in selectedHistoryKeys) {
                                    selectedHistoryKeys.add(itemKey)
                                }
                            },
                            onDelete = {
                                history = history.filterNot { it.time == item.time && it.prompt == item.prompt }
                                saveHistory(prefs, history)
                                selectedHistoryKeys.remove(itemKey)
                                status = "已删除该条图片记录。"
                            },
                            onCopyError = {
                                copyTextToClipboard(context, "ImageForge Error", item.error)
                                status = "错误详情已复制。"
                            },
                            onShare = { shareImageFromHistory(context, item.path) }
                        )
                    }
                }
                }
                // Floating top-right select-all button (only in selection mode)
                if (selectedHistoryKeys.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedHistoryKeys.clear()
                            selectedHistoryKeys.addAll(history.map { "${it.time}|${it.prompt}" })
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFE0E7FF))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("全选", color = Color(0xFF3730A3), fontWeight = FontWeight.Bold)
                    }
                }
                // Floating bottom-right delete FAB (only in selection mode)
                if (selectedHistoryKeys.isNotEmpty()) {
                    Surface(
                        onClick = {
                            val keys = selectedHistoryKeys.toSet()
                            history = history.filterNot { "${it.time}|${it.prompt}" in keys }
                            saveHistory(prefs, history)
                            selectedHistoryKeys.clear()
                            status = "已删除选中的图片记录。"
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 24.dp, end = 20.dp)
                            .size(60.dp),
                        shape = CircleShape,
                        color = Color(0xFFE11D48),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🗑", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
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
    BackHandler(enabled = true) {
        // 引导页不响应系统返回键，避免误触直接回到主页面。
    }
    var page by remember { mutableIntStateOf(0) }
    val totalPages = 4
    val canSave = baseUrl.isNotBlank() && apiKey.isNotBlank()

    Scaffold(containerColor = pageBg) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ImageForge",
                    color = Color(0xFF111827),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onSkip) {
                    Text("跳过", color = Color(0xFF6B7280), fontWeight = FontWeight.SemiBold)
                }
            }

            LinearProgressIndicator(
                progress = (page + 1).toFloat() / totalPages.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = accent,
                trackColor = Color(0xFFE4E8F5)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF8FAFF)),
                    shape = RoundedCornerShape(36.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 22.dp, vertical = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (page) {
                            0 -> OobeIntroPage()
                            1 -> OobeFeaturePage()
                            2 -> OobeConfigPage(
                                baseUrl = baseUrl,
                                apiKey = apiKey,
                                onBaseUrlChange = onBaseUrlChange,
                                onApiKeyChange = onApiKeyChange
                            )
                            else -> OobeReadyPage(canSave = canSave)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (page > 0) {
                    TextButton(
                        onClick = { page-- },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Text("上一步")
                    }
                }

                Button(
                    onClick = {
                        if (page < totalPages - 1) {
                            page++
                        } else if (canSave) {
                            onSave()
                        } else {
                            onSkip()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.56f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(
                        text = when {
                            page < totalPages - 1 -> "继续"
                            canSave -> "保存并开始"
                            else -> "进入应用"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OobeIntroPage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFE8EEFF)),
            contentAlignment = Alignment.Center
        ) {
            Text("AI", color = accent, fontSize = 34.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "欢迎使用通用图像工坊",
            color = Color(0xFF111827),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "选择接口，输入提示词，即可开始生成。",
            color = Color(0xFF6B7280),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun OobeFeaturePage() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "清爽风格界面",
            color = Color(0xFF111827),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "大圆角卡片、浅色层级和柔和主色，减少干扰。",
            color = Color(0xFF6B7280),
            lineHeight = 22.sp
        )
        OobeFeatureItem("生成与编辑", "文生图、图生图集中处理。")
        OobeFeatureItem("后台任务", "生成中也可以切换页面。")
        OobeFeatureItem("历史记录", "完成后可查看、下载和分享。")
    }
}

@Composable
private fun OobeFeatureItem(title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF0F3FF))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = accent, fontWeight = FontWeight.Black)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
            Text(description, color = Color(0xFF6B7280), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun OobeConfigPage(
    baseUrl: String,
    apiKey: String,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "配置接口",
            color = Color(0xFF111827),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "填写 Base URL 和 API Key，之后也可以在设置里修改。",
            color = Color(0xFF6B7280),
            lineHeight = 22.sp
        )
        OutlinedTextField(
            value = baseUrl,
            onValueChange = onBaseUrlChange,
            label = { Text("Base URL") },
            placeholder = { Text("例如：https://api.openai.com/v1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = onApiKeyChange,
            label = { Text("API Key") },
            placeholder = { Text("输入你的密钥") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
private fun OobeReadyPage(canSave: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(if (canSave) successBg else Color(0xFFFFF7ED)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (canSave) "✓" else "!",
                color = if (canSave) successText else Color(0xFFD97706),
                fontSize = 38.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = if (canSave) "准备完成" else "可以稍后配置",
            color = Color(0xFF111827),
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (canSave) {
                "保存后即可开始创作。"
            } else {
                "也可以先进入应用，稍后在设置页补充。"
            },
            color = Color(0xFF6B7280),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val context = androidx.compose.ui.platform.LocalContext.current
    Scaffold(
        containerColor = pageBg
    ) { padding ->
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
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

                    HorizontalDivider(color = DividerDefaults.color.copy(alpha = 0.55f))

                    SectionTitle("软件信息", "版本、作者和开源仓库")

                    val developerAvatar by produceState<Bitmap?>(initialValue = null) {
                        value = loadDeveloperAvatarBitmap()
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { openDeveloperQQ(context) },
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(accent.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (developerAvatar != null) {
                                        Image(
                                            bitmap = developerAvatar!!.asImageBitmap(),
                                            contentDescription = "开发者 QQ 头像",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    } else {
                                        Text(
                                            text = "QQ",
                                            color = accent,
                                            fontWeight = FontWeight.Black,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "开发者 QQ",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = DEVELOPER_QQ,
                                        color = Color(0xFF6B7280),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Text(
                                text = "›",
                                fontSize = 28.sp,
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "应用名称",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "ImageForge / 通用图像工坊",
                                color = Color(0xFF6B7280),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "版本：$IMAGEFORGE_VERSION_NAME",
                                color = Color(0xFF6B7280),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { openImageForgeRepository(context) },
                                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                shape = RoundedCornerShape(14.dp),
                                tonalElevation = 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "开源仓库",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = IMAGEFORGE_REPO_URL,
                                            color = Color(0xFF6B7280),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "›",
                                        fontSize = 24.sp,
                                        color = accent,
                                        fontWeight = FontWeight.SemiBold
                                    )
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

