package com.example.txt2img.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.txt2img.data.AppPrefs
import com.example.txt2img.data.ImageStore
import com.example.txt2img.data.PrefsJson
import com.example.txt2img.net.GenRequest
import com.example.txt2img.net.ImageClient
import com.example.txt2img.net.RefImage
import com.example.txt2img.notif.Notifier
import com.example.txt2img.service.KeepAliveService
import com.example.txt2img.ui.theme.Palette
import com.liquidglass.ui.modifier.isLiquidGlassEnabled
import com.liquidglass.ui.modifier.liquidGlass
import com.example.txt2img.util.SystemUtils
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_PROMPT = 500

private val SAMPLE_PROMPTS = listOf(
    "清晨的海边，阳光洒在海面上，波光粼粼，远处有帆船，天空中有海鸥飞翔，治愈系风格",
    "雨后的小巷，青石板路倒映着暖黄路灯，一只橘猫蹲在屋檐下躲雨，胶片质感",
    "未来城市的悬浮花园，透明玻璃穹顶内绿植环绕，晨光穿过薄雾，赛博植物美学",
    "冬日窗边，一杯热可可冒着热气，窗外飘雪，暖色调，宫崎骏治愈画风",
)

// 比例 → 服务端 size（与选择器顺序一致）
private val RATIO_OPTIONS = listOf("1:1", "3:4", "4:3", "9:16", "16:9")
private val RATIO_SIZES = listOf("1024x1024", "768x1024", "1024x768", "720x1280", "1280x720")

// 风格（可为无风格）→ prompt 注入模板；选择风格后可不填提示词直接生图
private val STYLE_OPTIONS = listOf(
    "无风格" to "",
    "轻度美颜" to "，保持人物面部结构、五官轮廓完全不变，仅轻微提亮肤色、柔化皮肤质感，去除明显瑕疵，自然真实，不能改变脸型与五官比例，subtle skin retouching only",
    "风景美化" to "，增强色彩通透度与光影层次，天空更通透、水面更清澈，保留景物原有结构，不过度处理，enhanced landscape with natural color and light",
    "主体突出" to "，主体清晰锐利，背景适度虚化，强化主体光影对比与质感，构图聚焦于画面主体，subject-focused with bokeh background",
    "二次元" to "，动漫二次元风格，线条干净，色彩明快，日系插画质感，anime style illustration",
    "漫画" to "，美式漫画风格，粗线条描边，高对比色块，网点阴影，comic book style",
    "卡通" to "，卡通风格，圆润造型，明快配色，可爱亲和，cartoon style",
    "插画" to "，现代插画风格，构图干净，扁平化设计，层次分明，modern flat illustration",
    "艺术" to "，艺术化处理，油画质感，笔触明显，光影富有表现力，fine art oil painting style",
    "水彩" to "，水彩画风格，柔和晕染，通透轻盈，纸张纹理，watercolor style",
)

// 清晰度 → (OpenAI quality 参数, 推理步数)
private val QUALITY_OPTIONS = listOf("标清", "高清", "超清")
private fun qualityFor(index: Int): Pair<String?, Int?> = when (index) {
    0 -> "low" to 20
    1 -> "high" to 30
    else -> "high" to 50
}

private val COUNT_OPTIONS = listOf("1 张", "2 张", "4 张")
private val COUNT_VALUES = listOf(1, 2, 4)

private sealed class GenState {
    object Idle : GenState()
    object Loading : GenState()
    data class Ready(val files: List<String>, val prompt: String) : GenState()
    data class Failed(val msg: String) : GenState()
}

private val ErrorRed = Color(0xFFC2473F)

@Composable
fun GenerateScreen(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val store = remember { ImageStore(context) }

    var prompt by rememberSaveable { mutableStateOf("") }
    var ratioIndex by rememberSaveable { mutableIntStateOf(0) }
    var styleIndex by rememberSaveable { mutableIntStateOf(0) }
    var qualityIndex by rememberSaveable { mutableIntStateOf(1) }
    var countIndex by rememberSaveable { mutableIntStateOf(0) }
    var refImages by remember { mutableStateOf<List<File>>(emptyList()) }
    var genState by remember { mutableStateOf<GenState>(GenState.Idle) }
    var showModelPicker by remember { mutableStateOf(false) }
    var showPromptEditor by remember { mutableStateOf(false) }
    var pickedCat by remember { mutableStateOf("") }
    var agentEnabled by rememberSaveable { mutableStateOf(false) }
    var showAgentPicker by remember { mutableStateOf(false) }
    var showReversePicker by remember { mutableStateOf(false) }
    var optimizing by remember { mutableStateOf(false) }
    var optimizeDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var reversing by remember { mutableStateOf(false) }
    var reverseDialog by remember { mutableStateOf<ImageClient.ReverseResult?>(null) }
    var failCount by remember { mutableIntStateOf(0) }

    val providersJson by prefs.providersJson.collectAsState(initial = "[]")
    val currentJson by prefs.currentJson.collectAsState(initial = "{}")
    val catsJson by prefs.catsJson.collectAsState(initial = "[]")
    val imagesJson by prefs.imagesJson.collectAsState(initial = "[]")

    val providers = remember(providersJson) { PrefsJson.parseProviders(providersJson) }
    val current = remember(currentJson) { PrefsJson.parseCurrent(currentJson) }
    val cats = remember(catsJson) { PrefsJson.parseCats(catsJson) }
    val activeProvider = providers.find { it.id == current.providerId }
    val baseUrl = activeProvider?.url.orEmpty()
    val apiKey = activeProvider?.key.orEmpty()
    val selectedModel = current.model

    val agentJson by prefs.agentJson.collectAsState(initial = "{}")
    val agentSel = remember(agentJson) { PrefsJson.parseCurrent(agentJson) }
    val visionJson by prefs.visionJson.collectAsState(initial = "{}")
    val reverseModelJson by prefs.reverseModelJson.collectAsState(initial = "{}")
    val reverseSel = remember(reverseModelJson) { PrefsJson.parseCurrent(reverseModelJson) }
    val reverseProvider = providers.find { it.id == reverseSel.providerId }
    val reverseBaseUrl = reverseProvider?.url.orEmpty()
    val reverseApiKey = reverseProvider?.key.orEmpty()
    val reverseModel = reverseSel.model
    val reverseEnabled by prefs.reverseEnabled.collectAsState(initial = false)
    val failLimit by prefs.failLimit.collectAsState(initial = 3)
    // 视觉能力绑定反推模型（与生图/Agent 模型完全独立）
    val reverseVisionOk = remember(visionJson, reverseSel.providerId, reverseSel.model) {
        if (reverseSel.providerId.isEmpty() || reverseSel.model.isEmpty()) {
            false
        } else {
            try {
                org.json.JSONObject(visionJson).optString("${reverseSel.providerId}|${reverseSel.model}", "") == "yes"
            } catch (e: Exception) {
                false
            }
        }
    }

    val scope = rememberCoroutineScope()

    // 二次编辑协调逻辑：载入原图作参考图 + 载入提示词 → 返回生成页现场微调
    val startEdit: (String, File) -> Unit = { editPrompt, imageFile ->
        prompt = editPrompt.take(MAX_PROMPT)
        styleIndex = 0
        refImages = listOf(imageFile)
        genState = GenState.Idle
        onTabSelected(0)
        Toast.makeText(context, "已载入参考图，可修改提示词后重新生成", Toast.LENGTH_SHORT).show()
    }

    // 进入应用即请求通知权限（Android 13+）
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}
    LaunchedEffect(Unit) {
        if (!SystemUtils.hasNotificationPermission(context)) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // 首次启动引导开启后台运行保护（电池优化豁免）
    val askedBattery by prefs.askedBattery.collectAsState(initial = false)
    var showBatteryTip by remember { mutableStateOf(false) }
    LaunchedEffect(askedBattery) {
        if (!askedBattery && SystemUtils.isBatteryOptimized(context)) {
            showBatteryTip = true
            prefs.markAskedBattery()
        }
    }
    if (showBatteryTip) {
        TipDialog(
            title = "后台运行保护",
            message = "生成图片通常需要几秒到几十秒。允许忽略电池优化后，即使切到其他应用或锁屏，生成任务也能继续完成，不会被系统中断。是否现在开启？",
            confirmText = "去开启",
            onConfirm = {
                SystemUtils.requestBatteryExemption(context)
                showBatteryTip = false
            },
            onDismiss = { showBatteryTip = false },
        )
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val f = copyUriToCache(context, uri)
            if (f == null) {
                genState = GenState.Failed("读取参考图失败")
            } else if (refImages.size < MAX_REFS) {
                refImages = refImages + f
            } else {
                genState = GenState.Failed("最多支持 $MAX_REFS 张参考图")
            }
        }
    }

    ScreenBackdrop {
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.985f,
                            animationSpec = tween(260, easing = FastOutSlowInEasing),
                        )
                    )
                    .togetherWith(
                        fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing)),
                    )
            },
            label = "tabContent",
        ) { tab ->
            when (tab) {
                0 -> GeneratePage(
                    prompt = prompt,
                    onPromptChange = { prompt = it },
                    ratioIndex = ratioIndex,
                    onRatioSelect = { ratioIndex = it },
                    styleIndex = styleIndex,
                    onStyleSelect = { styleIndex = it },
                    qualityIndex = qualityIndex,
                    onQualitySelect = { qualityIndex = it },
                    countIndex = countIndex,
                    onCountSelect = { countIndex = it },
                    refImages = refImages,
                    onAddRef = { picker.launch("image/*") },
                    onRemoveRef = { i -> refImages = refImages.filterIndexed { idx, _ -> idx != i } },
                    modelName = selectedModel,
                    genState = genState,
                    agentEnabled = agentEnabled,
                    agentModelName = if (agentSel.model.isBlank()) "" else agentSel.model,
                    optimizing = optimizing,
                    visionOk = reverseVisionOk,
                    reverseModelName = if (reverseModel.isBlank()) "" else reverseModel,
                    reversing = reversing,
                    reverseEnabled = reverseEnabled,
                    onToggleReverse = { scope.launch { prefs.saveReverseEnabled(!reverseEnabled) } },
                    onPickReverseModel = {
                        if (providers.isEmpty()) {
                            Toast.makeText(context, "请先添加供应商", Toast.LENGTH_SHORT).show()
                            onTabSelected(2)
                        } else {
                            showReversePicker = true
                        }
                    },
                    onOpenEditor = { showPromptEditor = true },
                    onToggleAgent = { agentEnabled = !agentEnabled },
                    onOpenAgentPicker = {
                        if (providers.isEmpty()) {
                            Toast.makeText(context, "请先添加供应商", Toast.LENGTH_SHORT).show()
                            onTabSelected(2)
                        } else {
                            showAgentPicker = true
                        }
                    },
                    onReverse = {
                        if (!reverseEnabled) {
                            // 功能开关关闭，仅提示
                            Toast.makeText(context, "反推功能未开启，请先打开开关", Toast.LENGTH_SHORT).show()
                        } else if (refImages.isEmpty()) {
                            Toast.makeText(context, "请先上传参考图", Toast.LENGTH_SHORT).show()
                        } else if (reverseProvider == null || reverseModel.isBlank()) {
                            Toast.makeText(context, "请先选择反推模型", Toast.LENGTH_SHORT).show()
                            showReversePicker = true
                        } else if (!reverseVisionOk) {
                            Toast.makeText(context, "该模型未通过视觉测试，请选择其他多模态模型", Toast.LENGTH_SHORT).show()
                            showReversePicker = true
                        } else if (reversing) {
                            // 进行中忽略
                        } else {
                            reversing = true
                            // 反推期间保活：切后台/锁屏不中断
                            KeepAliveService.start(context)
                            val refFile = refImages.first()
                            scope.launch {
                                val mime = mimeOf(refFile)
                                val dataUri = "data:$mime;base64," +
                                    android.util.Base64.encodeToString(refFile.readBytes(), android.util.Base64.NO_WRAP)
                                ImageClient.reversePrompt(reverseBaseUrl, reverseApiKey, reverseModel, dataUri)
                                    .onSuccess { r ->
                                        reverseDialog = r
                                        Notifier.notifyReverseDone(context, r.category, r.prompt)
                                    }
                                    .onFailure { e ->
                                        Toast.makeText(context, "反推失败：${e.message?.take(80)}", Toast.LENGTH_SHORT).show()
                                    }
                                KeepAliveService.stop(context)
                                reversing = false
                            }
                        }
                    },
                    onGenerate = {
                        if (activeProvider == null || baseUrl.isBlank() || apiKey.isBlank() || selectedModel.isBlank()) {
                            genState = GenState.Failed("请先在「我的」中配置供应商并选择模型")
                            return@GeneratePage
                        }
                        // 已达连续失败上限：重置并重新尝试
                        if (failCount >= failLimit) {
                            failCount = 0
                            Toast.makeText(context, "已重置连续失败计数，重新尝试生成", Toast.LENGTH_SHORT).show()
                        }
                        val styleOnly = STYLE_OPTIONS[styleIndex].second.removePrefix("，")
                            .ifEmpty { SAMPLE_PROMPTS.first() }
                        val userPrompt = prompt.trim().ifEmpty { styleOnly }
                        val (qualityParam, steps) = qualityFor(qualityIndex)

                        val doGenerate: (String) -> Unit = { userText ->
                            val fullPrompt = userText + STYLE_OPTIONS[styleIndex].second
                            genState = GenState.Loading
                            // 前台服务保活：切后台/锁屏不中断生成
                            KeepAliveService.start(context)
                            scope.launch {
                                val t0 = System.currentTimeMillis()
                                val refs = refImages.map { f ->
                                    RefImage(f.readBytes(), mimeOf(f))
                                }
                                ImageClient.generate(
                                    GenRequest(
                                        baseUrl = baseUrl,
                                        apiKey = apiKey,
                                        model = selectedModel,
                                        prompt = fullPrompt,
                                        count = COUNT_VALUES[countIndex],
                                        size = RATIO_SIZES[ratioIndex],
                                        qualityParam = qualityParam,
                                        steps = steps,
                                        refImages = refs,
                                    ),
                                ).onSuccess { outcome ->
                                    val elapsed = System.currentTimeMillis() - t0
                                    failCount = 0
                                    val saved = mutableListOf<String>()
                                    val now = System.currentTimeMillis()
                                    val refName = refs.firstOrNull()?.let { store.saveRef(it.bytes, it.mime) }
                                    outcome.images.forEachIndexed { idx, bytes ->
                                        store.save(bytes)?.let { name ->
                                            prefs.appendImage(
                                                prompt = fullPrompt,
                                                time = now + idx,
                                                file = name,
                                                refFile = refName,
                                                durationMs = elapsed,
                                                ratio = RATIO_OPTIONS[ratioIndex],
                                            )
                                            saved.add(name)
                                        }
                                    }
                                    KeepAliveService.stop(context)
                                    if (saved.isNotEmpty()) {
                                        genState = GenState.Ready(saved, fullPrompt)
                                        Notifier.notifyGenerationDone(context, saved.size, fullPrompt)
                                    } else {
                                        genState = GenState.Failed("图片保存失败")
                                    }
                                }.onFailure { e ->
                                    KeepAliveService.stop(context)
                                    val msg = e.message ?: "生成失败"
                                    failCount++
                                    scope.launch { prefs.saveLastError(msg) }
                                    genState = if (failCount >= failLimit) {
                                        GenState.Failed("连续失败 $failCount 次，已停止生成。请前往「我的」查看失败详情")
                                    } else {
                                        GenState.Failed("生成失败（第 $failCount/$failLimit 次）：${msg.take(120)}")
                                    }
                                }
                            }
                        }

                        if (agentEnabled) {
                            // Agent 模式：先扶正提示词，预览后应用/暂不应用
                            if (optimizing) return@GeneratePage
                            val ap = providers.find { it.id == agentSel.providerId }
                            if (ap == null || agentSel.model.isBlank()) {
                                Toast.makeText(context, "请先选择扶正模型", Toast.LENGTH_SHORT).show()
                                showAgentPicker = true
                                return@GeneratePage
                            }
                            optimizing = true
                            scope.launch {
                                ImageClient.optimizePrompt(ap.url, ap.key, agentSel.model, userPrompt)
                                    .onSuccess { opt ->
                                        optimizing = false
                                        optimizeDialog = userPrompt to opt
                                    }
                                    .onFailure { e ->
                                        optimizing = false
                                        genState = GenState.Failed("提示词扶正失败：${e.message?.take(80)}")
                                    }
                            }
                        } else {
                            doGenerate(userPrompt)
                        }
                    },
                    onModelClick = { showModelPicker = true },
                )

                1 -> GalleryTab(
                    imagesJson = imagesJson,
                    catsJson = catsJson,
                    store = store,
                    prefs = prefs,
                    onEdit = { p, f -> startEdit(p, f) },
                )

                else -> MyTab(prefs = prefs)
            }
        }
    }

    // 生成结果弹窗（多图），关闭/微调时应用所选分类
    val ready = genState as? GenState.Ready
    if (ready != null) {
        fun applyCat() {
            if (ready.files.isNotEmpty()) {
                scope.launch { ready.files.forEach { prefs.setImageCategory(it, pickedCat) } }
            }
        }
        val onEdit: () -> Unit = {
            applyCat()
            startEdit(ready.prompt, store.fileFor(ready.files.first()))
        }
        if (ready.files.size > 1) {
            MultiImageDialog(
                images = ready.files.map { store.fileFor(it) },
                prompt = ready.prompt,
                onDismiss = {
                    applyCat()
                    genState = GenState.Idle
                },
                onEdit = onEdit,
                categories = cats,
                pickedCat = pickedCat,
                onPickCategory = { pickedCat = it },
            )
        } else {
            GlassImageDialog(
                file = store.fileFor(ready.files.first()),
                prompt = ready.prompt,
                time = 0L,
                onDismiss = {
                    applyCat()
                    genState = GenState.Idle
                },
                onEdit = onEdit,
                categories = cats,
                pickedCat = pickedCat,
                onPickCategory = { pickedCat = it },
            )
        }
    }
    // 提示词放大编辑面板
    if (showPromptEditor) {
        PromptEditorDialog(
            prompt = prompt,
            onConfirm = { p ->
                prompt = p
                showPromptEditor = false
            },
            onDismiss = { showPromptEditor = false },
        )
    }

    // 模型快捷选择面板（生成模型，与 Agent/反推模型完全独立）
    if (showModelPicker) {
        ModelPickerDialog(
            providers = providers,
            current = current,
            onPick = { pid, m ->
                scope.launch {
                    prefs.saveCurrent(pid, m)
                    prefs.saveProviders(
                        providers.map { if (it.id == pid) it.copy(selectedModel = m) else it },
                    )
                }
                showModelPicker = false
            },
            onGoConfig = {
                showModelPicker = false
                onTabSelected(2)
            },
            onDismiss = { showModelPicker = false },
        )
    }

    // Agent 扶正模型选择面板（独立）
    if (showAgentPicker) {
        ModelPickerDialog(
            providers = providers,
            current = agentSel,
            title = "选择扶正模型",
            subtitle = "任意供应商的模型均可用于提示词优化",
            onPick = { pid, m ->
                scope.launch { prefs.saveAgent(pid, m) }
                showAgentPicker = false
            },
            onGoConfig = {
                showAgentPicker = false
                onTabSelected(2)
            },
            onDismiss = { showAgentPicker = false },
        )
    }

    // 反推模型选择面板（独立，选中后自动视觉测试）
    if (showReversePicker) {
        ModelPickerDialog(
            providers = providers,
            current = reverseSel,
            title = "选择反推模型",
            subtitle = "需支持图片识别的多模态模型，选中后自动测试",
            onPick = { pid, m ->
                val key = "$pid|$m"
                val cached = try {
                    org.json.JSONObject(visionJson).optString(key, "")
                } catch (e: Exception) {
                    ""
                }
                if (cached.isEmpty()) {
                    val p = providers.find { it.id == pid }
                    if (p != null) {
                        scope.launch {
                            val ok = ImageClient.testVision(p.url, p.key, m).isSuccess
                            prefs.saveVisionResult(key, ok)
                            Toast.makeText(
                                context,
                                if (ok) "该模型支持图片识别，已启用反推" else "该模型不支持图片识别，请换一个模型",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
                scope.launch { prefs.saveReverseModel(pid, m) }
                showReversePicker = false
            },
            onGoConfig = {
                showReversePicker = false
                onTabSelected(2)
            },
            onDismiss = { showReversePicker = false },
        )
    }

    // 扶正结果预览：应用仅回填提示词框，生成需再点「开始生成」
    optimizeDialog?.let { (orig, opt) ->
        OptimizePreviewDialog(
            original = orig,
            optimized = opt,
            onApply = {
                prompt = opt.take(MAX_PROMPT)
                agentEnabled = false
                optimizeDialog = null
                Toast.makeText(context, "已应用优化后的提示词，点击「开始生成」继续", Toast.LENGTH_SHORT).show()
            },
            onCancel = { optimizeDialog = null },
        )
    }

    // 反推结果弹窗
    reverseDialog?.let { r ->
        val sourceFile = refImages.firstOrNull()
        if (sourceFile != null) {
            ReverseResultDialog(
                sourceFile = sourceFile,
                category = r.category,
                prompt = r.prompt,
                onCopy = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("reverse", r.prompt))
                    Toast.makeText(context, "反推提示词已复制", Toast.LENGTH_SHORT).show()
                },
                onApplyBox = {
                    prompt = r.prompt.take(MAX_PROMPT)
                    reverseDialog = null
                    Toast.makeText(context, "已应用到生成框，可直接生图", Toast.LENGTH_SHORT).show()
                },
                onSave = {
                    scope.launch {
                        val name = withContext(Dispatchers.IO) {
                            store.save(sourceFile.readBytes())
                        }
                        if (name != null) {
                            prefs.appendImage(
                                prompt = r.prompt,
                                time = System.currentTimeMillis(),
                                file = name,
                                type = "reverse",
                            )
                        }
                    }
                    reverseDialog = null
                    Toast.makeText(context, "已保存到作品页", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { reverseDialog = null },
            )
        }
    }
}

// ============ 生成页 ============

@Composable
private fun GeneratePage(
    prompt: String,
    onPromptChange: (String) -> Unit,
    ratioIndex: Int,
    onRatioSelect: (Int) -> Unit,
    styleIndex: Int,
    onStyleSelect: (Int) -> Unit,
    qualityIndex: Int,
    onQualitySelect: (Int) -> Unit,
    countIndex: Int,
    onCountSelect: (Int) -> Unit,
    refImages: List<File>,
    onAddRef: () -> Unit,
    onRemoveRef: (Int) -> Unit,
    modelName: String,
    genState: GenState,
    agentEnabled: Boolean,
    agentModelName: String,
    optimizing: Boolean,
    visionOk: Boolean,
    reverseModelName: String,
    reversing: Boolean,
    reverseEnabled: Boolean,
    onToggleReverse: () -> Unit,
    onPickReverseModel: () -> Unit,
    onToggleAgent: () -> Unit,
    onOpenAgentPicker: () -> Unit,
    onReverse: () -> Unit,
    onOpenEditor: () -> Unit,
    onGenerate: () -> Unit,
    onModelClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 64.dp)
            .padding(bottom = 76.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(8.dp))
        PromptCard(
            prompt = prompt,
            onPromptChange = onPromptChange,
            onOpenEditor = onOpenEditor,
        )
        Spacer(Modifier.height(8.dp))
        RefImagesRow(refs = refImages, onAdd = onAddRef, onRemove = onRemoveRef)
        Spacer(Modifier.height(8.dp))
        ParamPanel(
            modelName = modelName,
            ratioIndex = ratioIndex,
            onRatioSelect = onRatioSelect,
            styleIndex = styleIndex,
            onStyleSelect = onStyleSelect,
            qualityIndex = qualityIndex,
            onQualitySelect = onQualitySelect,
            countIndex = countIndex,
            onCountSelect = onCountSelect,
            onModelClick = onModelClick,
        )
        Spacer(Modifier.height(10.dp))
        ReverseRow(
            enabled = reverseEnabled,
            visionOk = visionOk,
            modelName = reverseModelName,
            reversing = reversing,
            onToggle = onToggleReverse,
            onPickModel = onPickReverseModel,
            onClick = onReverse,
        )
        Spacer(Modifier.height(8.dp))
        AgentBar(
            enabled = agentEnabled,
            modelName = agentModelName,
            optimizing = optimizing,
            onToggle = onToggleAgent,
            onPickModel = onOpenAgentPicker,
        )
        Spacer(Modifier.height(10.dp))
        GenerateButton(
            loading = genState is GenState.Loading || optimizing,
            onClick = onGenerate,
        )
        when (val s = genState) {
            is GenState.Failed -> {
                Spacer(Modifier.height(6.dp))
                Text(
                    s.msg,
                    fontSize = 9.sp,
                    color = ErrorRed,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            is GenState.Loading -> {
                Spacer(Modifier.height(6.dp))
                Text(
                    "正在生成，请稍候…",
                    fontSize = 9.sp,
                    color = Palette.InkMid,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
            else -> {}
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ============ 顶部状态栏 & Header ============

@Composable
private fun Header() {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val headerAlpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "headerAlpha",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .graphicsLayer { alpha = headerAlpha }
            .padding(start = 2.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "文字生图",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkTitle,
                letterSpacing = 0.2.sp,
            )
            Text(
                "输入描述词，AI 帮你生成精美图片",
                fontSize = 9.sp,
                color = Palette.InkLight,
            )
        }
    }
}

// ============ 提示词输入卡片 ============

@Composable
private fun PromptCard(prompt: String, onPromptChange: (String) -> Unit, onOpenEditor: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .realGlassCard(RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Brush,
                contentDescription = null,
                tint = Palette.InkStrong,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                "描述你的画面",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Filled.OpenInFull,
                contentDescription = "展开编辑",
                tint = Palette.InkMid,
                modifier = Modifier
                    .size(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenEditor),
            )
        }
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .realGlassField(RoundedCornerShape(8.dp))
                .padding(8.dp),
        ) {
            Box(Modifier.weight(1f)) {
                if (prompt.isEmpty()) {
                    Text(
                        SAMPLE_PROMPTS.first(),
                        fontSize = 10.sp,
                        color = Palette.InkStrong,
                        lineHeight = 14.sp,
                    )
                }
                BasicTextField(
                    value = prompt,
                    onValueChange = { if (it.length <= MAX_PROMPT) onPromptChange(it) },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(
                        fontSize = 10.sp,
                        color = Palette.InkStrong,
                        lineHeight = 14.sp,
                    ),
                    maxLines = 2,
                    cursorBrush = SolidColor(Palette.Purple),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .height(20.dp)
                        .realGlassChip(RoundedCornerShape(10.dp))
                        .clickable { onPromptChange(SAMPLE_PROMPTS.random()) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Casino,
                        contentDescription = null,
                        tint = Palette.InkMid,
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "随机灵感",
                        fontSize = 9.sp,
                        color = Palette.InkMid,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${prompt.length}/$MAX_PROMPT",
                    fontSize = 8.sp,
                    color = Palette.InkLight,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "清空",
                    fontSize = 9.sp,
                    color = Palette.InkMid,
                    modifier = Modifier.clickable { onPromptChange("") },
                )
            }
        }
    }
}

// ============ 参考图（多张联动，上限 3） ============

private const val MAX_REFS = 3

@Composable
private fun RefImagesRow(
    refs: List<File>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .realGlassCard(RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Image,
            contentDescription = null,
            tint = Palette.InkMid,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "参考图",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.InkStrong,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "${refs.size}/$MAX_REFS",
            fontSize = 8.sp,
            color = Palette.InkLight,
        )
        Spacer(Modifier.weight(1f))
        refs.forEachIndexed { i, f ->
            Box(Modifier.size(34.dp)) {
                AsyncImage(
                    model = f,
                    contentDescription = "参考图 ${i + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(6.dp)),
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(14.dp)
                        .realGlassChip(RoundedCornerShape(7.dp))
                        .clickable { onRemove(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "移除参考图 ${i + 1}",
                        tint = Palette.InkMid,
                        modifier = Modifier.size(8.dp),
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
        }
        if (refs.size < MAX_REFS) {
            Box(
                Modifier
                    .realGlassChip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Text(
                    "＋添加",
                    fontSize = 9.sp,
                    color = Palette.InkMid,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ============ 生成参数配置面板 ============

@Composable
private fun ParamPanel(
    modelName: String,
    ratioIndex: Int,
    onRatioSelect: (Int) -> Unit,
    styleIndex: Int,
    onStyleSelect: (Int) -> Unit,
    qualityIndex: Int,
    onQualitySelect: (Int) -> Unit,
    countIndex: Int,
    onCountSelect: (Int) -> Unit,
    onModelClick: () -> Unit,
) {
    var styleExpanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .realGlassCard(RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        // 模型（点击跳转「我的」）
        Row(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .glassPressable()
                .clickable(onClick = onModelClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.GridView,
                contentDescription = null,
                tint = Palette.InkMid,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "模型",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.weight(1f))
            Text(
                if (modelName.isBlank()) "去配置 ＞" else "$modelName ＞",
                fontSize = 10.sp,
                color = if (modelName.isBlank()) Palette.InkLight else Palette.InkStrong,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Hairline()

        // 风格（展开选择，可为无风格）
        Row(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .glassPressable()
                .clickable { styleExpanded = !styleExpanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.ColorLens,
                contentDescription = null,
                tint = Palette.InkMid,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "风格",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${STYLE_OPTIONS[styleIndex].first} ＞",
                fontSize = 10.sp,
                color = Palette.InkStrong,
            )
        }
        AnimatedContent(
            targetState = styleExpanded,
            transitionSpec = {
                (
                    fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                        scaleIn(
                            initialScale = 0.97f,
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                        )
                    )
                    .togetherWith(fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing)))
            },
            label = "styleChips",
        ) { expanded ->
            if (expanded) {
                StyleChips(selected = styleIndex, onSelect = onStyleSelect)
            }
        }
        Hairline()

        RatioRow(selected = ratioIndex, onSelect = onRatioSelect)
        Hairline()

        // 图片质量
        ParamRowWithSelector(
            icon = Icons.Filled.Layers,
            label = "图片质量",
            options = QUALITY_OPTIONS,
            selected = qualityIndex,
            onSelect = onQualitySelect,
        )
        Hairline()

        // 生成数量
        ParamRowWithSelector(
            icon = Icons.Filled.Collections,
            label = "生成数量",
            options = COUNT_OPTIONS,
            selected = countIndex,
            onSelect = onCountSelect,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleChips(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        STYLE_OPTIONS.forEachIndexed { i, (label, _) ->
            val isSel = i == selected
            val selScale by animateFloatAsState(
                targetValue = if (isSel) 1f else 0.92f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 600f),
                label = "chipSelScale",
            )
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = selScale
                        scaleY = selScale
                    }
                    .realGlassChip(RoundedCornerShape(10.dp))
                    .border(
                        width = 1.dp,
                        color = if (isSel) Palette.Purple else Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(i) }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    label,
                    fontSize = 9.sp,
                    color = if (isSel) Palette.Purple else Palette.InkMid,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun ParamRowWithSelector(
    icon: ImageVector,
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Palette.InkMid,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.InkStrong,
        )
        Spacer(Modifier.weight(1f))
        MiniSegmented(options = options, selected = selected, onSelect = onSelect)
    }
}

@Composable
private fun MiniSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .width(100.dp)
            .height(22.dp)
            .realGlassField(shape)
            .padding(1.dp),
    ) {
        options.forEachIndexed { i, o ->
            val isSel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(if (isSel) Color.White.copy(alpha = 0.55f) else Color.Transparent)
                    .border(
                        width = if (isSel) 1.dp else 0.dp,
                        color = Palette.Purple,
                        shape = shape,
                    )
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    o,
                    fontSize = 8.sp,
                    color = if (isSel) Palette.Purple else Palette.InkMid,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Palette.Divider),
    )
}

// 比例行：玻璃胶囊 + 点击切换（指示条线性滑动）
@Composable
private fun RatioRow(selected: Int, onSelect: (Int) -> Unit) {
    val ratios = RATIO_OPTIONS
    val indicatorX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        indicatorX.snapTo(selected * with(density) { (154.dp / ratios.size).toPx() })
    }
    LaunchedEffect(selected) {
        scope.launch {
            indicatorX.animateTo(
                selected * with(density) { (154.dp / ratios.size).toPx() },
                tween(180, easing = LinearEasing),
            )
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.CropSquare,
            contentDescription = null,
            tint = Palette.InkMid,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "比例",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.InkStrong,
        )
        Spacer(Modifier.weight(1f))

        BoxWithConstraints(
            Modifier
                .width(154.dp)
                .height(22.dp)
                .realGlassField(RoundedCornerShape(50))
                .padding(1.dp),
        ) {
            val seg = maxWidth / ratios.size
            val segPx = with(density) { seg.toPx() }

            Box(
                Modifier
                    .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                    .size(seg, 22.dp)
                    .padding(1.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.55f))
                    .border(1.dp, Palette.Purple, RoundedCornerShape(50)),
            )

            Row(Modifier.fillMaxSize()) {
                ratios.forEachIndexed { i, r ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                onSelect(i)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            r,
                            fontSize = 8.sp,
                            color = if (i == selected) Palette.Purple else Palette.InkMid,
                            fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

// ============ 图片反推（多模态） ============

@Composable
private fun ReverseRow(
    enabled: Boolean,
    visionOk: Boolean,
    modelName: String,
    reversing: Boolean,
    onToggle: () -> Unit,
    onPickModel: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .realGlassCard(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(26.dp)
                    .background(Color.White.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = Palette.Purple,
                    modifier = Modifier.size(13.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "图片反推提示词",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.InkStrong,
                )
                Text(
                    if (enabled) "独立反推模型 · 多模态识别" else "反推功能默认关闭",
                    fontSize = 8.sp,
                    color = Palette.InkLight,
                )
            }
            TogglePill(enabled = enabled, onToggle = onToggle)
        }
        if (enabled) {
            Spacer(Modifier.height(6.dp))
            // 反推模型选择（独立于生图/Agent）
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .realGlassRow(RoundedCornerShape(8.dp))
                    .clickable(onClick = onPickModel)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "反推模型",
                    fontSize = 9.sp,
                    color = Palette.InkMid,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (modelName.isBlank()) "未选择，点击选择 ＞" else "$modelName ＞",
                    fontSize = 9.sp,
                    color = if (modelName.isBlank()) Palette.InkLight else Palette.Purple,
                    fontWeight = if (modelName.isBlank()) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .realGlassRow(RoundedCornerShape(8.dp))
                    .clickable(enabled = !reversing, onClick = onClick)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        reversing -> "反推中，请稍候…"
                        modelName.isBlank() -> "选择模型后可开始"
                        visionOk -> "视觉模型就绪"
                        else -> "未通过视觉测试"
                    },
                    fontSize = 9.sp,
                    color = if (visionOk && modelName.isNotBlank()) Palette.Purple else Palette.InkMid,
                    fontWeight = if (visionOk && modelName.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    if (reversing) "…" else "开始反推 ＞",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.InkStrong,
                )
            }
        }
    }
}

// ============ Agent 提示词扶正 ============

@Composable
private fun AgentBar(
    enabled: Boolean,
    modelName: String,
    optimizing: Boolean,
    onToggle: () -> Unit,
    onPickModel: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .realGlassCard(RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = if (enabled) Palette.Purple else Palette.InkLight,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "Agent 提示词扶正",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Palette.InkStrong,
        )
        Spacer(Modifier.weight(1f))
        if (enabled) {
            if (optimizing) {
                Text(
                    "扶正中…",
                    fontSize = 9.sp,
                    color = Palette.Purple,
                )
            } else {
                Text(
                    if (modelName.isBlank()) "选择扶正模型 ＞" else "$modelName ＞",
                    fontSize = 9.sp,
                    color = Palette.Purple,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .glassPressable()
                        .clickable(onClick = onPickModel),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        TogglePill(enabled = enabled, onToggle = onToggle)
    }
}

@Composable
private fun TogglePill(enabled: Boolean, onToggle: () -> Unit) {
    Box(
        Modifier
            .width(34.dp)
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Palette.Purple else Color.White.copy(alpha = 0.5f))
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .glassPressable()
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (enabled) "开" else "关",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else Palette.InkMid,
        )
    }
}

// ============ 主操作生成按钮 ============

@Composable
private fun GenerateButton(loading: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale = remember { Animatable(1f) }
    var scaleVal by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        snapshotFlow { scale.value }.collect { scaleVal = it }
    }

    LaunchedEffect(pressed) {
        if (pressed) {
            scale.animateTo(0.96f, tween(100, easing = LinearEasing))
        } else {
            scale.animateTo(1f, tween(120, easing = LinearEasing))
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .graphicsLayer {
                scaleX = scaleVal
                scaleY = scaleVal
            }
            .background(Palette.ButtonBlue.copy(alpha = 0.88f), RoundedCornerShape(10.dp))
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.30f),
                            Color.Transparent,
                        ),
                        startY = 0f,
                        endY = size.height * 0.5f,
                    ),
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !loading,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (loading) "生成中…" else "开始生成",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = if (loading) 0.8f else 1f),
            letterSpacing = 0.3.sp,
        )
    }
}

// ============ 悬浮胶囊底部导航 ============

private data class NavTab(val label: String, val icon: ImageVector)

private val NAV_TABS = listOf(
    NavTab("生成", Icons.Filled.Draw),
    NavTab("作品", Icons.Filled.PhotoLibrary),
    NavTab("我的", Icons.Filled.Person),
)

private val NavInactive = Color(0xFF7E7A8F)

@Composable
fun FloatingNav(selected: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    var index by remember { mutableIntStateOf(selected) }

    val liquidEnabled = isLiquidGlassEnabled() &&
        com.liquidglass.ui.LocalGlassConfig.current.glassLevel >= 1
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .then(
                if (liquidEnabled) {
                    Modifier.liquidGlass(RoundedCornerShape(23.dp))
                } else {
                    Modifier.glassCard(RoundedCornerShape(23.dp))
                }
            ),
    ) {
        Row(Modifier.fillMaxSize()) {
            NAV_TABS.forEachIndexed { i, tab ->
                val isSelected = index == i
                val labelColor by animateColorAsState(
                    targetValue = if (isSelected) Palette.Purple else NavInactive,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    label = "navLabel",
                )

                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            index = i
                            onSelected(i)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.size(18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AnimatedContent(
                                targetState = isSelected,
                                transitionSpec = {
                                    (
                                        scaleIn(
                                            initialScale = 0.7f,
                                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                                        ) + fadeIn(tween(120, easing = FastOutSlowInEasing))
                                        )
                                        .togetherWith(fadeOut(tween(120, easing = FastOutSlowInEasing)))
                                },
                                label = "navIcon",
                            ) { sel ->
                                if (sel) {
                                    Box(
                                        Modifier
                                            .size(16.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(Palette.Purple),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            tab.icon,
                                            contentDescription = tab.label,
                                            tint = Color.White,
                                            modifier = Modifier.size(10.dp),
                                        )
                                    }
                                } else {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.label,
                                        tint = NavInactive,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            tab.label,
                            fontSize = if (isSelected) 10.sp else 9.sp,
                            color = labelColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

// ============ 工具 ============

private fun copyUriToCache(context: Context, uri: Uri): File? = try {
    val mime = context.contentResolver.getType(uri) ?: "image/png"
    val ext = when {
        mime.contains("jpeg") -> "jpg"
        mime.contains("webp") -> "webp"
        mime.contains("png") -> "png"
        mime.contains("gif") -> "gif"
        else -> "png"
    }
    val file = File(context.cacheDir, "ref_${System.currentTimeMillis()}_${System.nanoTime()}.$ext")
    val input = context.contentResolver.openInputStream(uri) ?: return null
    input.use { ins ->
        file.outputStream().use { outs -> ins.copyTo(outs) }
    }
    file
} catch (e: Exception) {
    null
}

private fun mimeOf(file: File): String = when (file.extension.lowercase()) {
    "webp" -> "image/webp"
    "jpg", "jpeg" -> "image/jpeg"
    "gif" -> "image/gif"
    else -> "image/png"
}
