package com.example.txt2img.ui

import android.Manifest
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
import com.example.txt2img.util.SystemUtils
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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

// 风格（可为无风格）→ prompt 后缀
private val STYLE_OPTIONS = listOf(
    "无风格" to "",
    "治愈系" to "，治愈系风格，色调温柔，画面清新治愈",
    "赛博朋克" to "，赛博朋克风格，霓虹灯光，未来都市，科技感",
    "胶片" to "，胶片摄影质感，颗粒感，复古色调",
    "水彩" to "，水彩画风格，柔和晕染，通透轻盈",
    "插画" to "，现代插画风格，构图干净，扁平化",
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
fun GenerateScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }
    val store = remember { ImageStore(context) }

    var currentTab by remember { mutableIntStateOf(0) }
    var prompt by rememberSaveable { mutableStateOf("") }
    var ratioIndex by rememberSaveable { mutableIntStateOf(0) }
    var styleIndex by rememberSaveable { mutableIntStateOf(0) }
    var qualityIndex by rememberSaveable { mutableIntStateOf(1) }
    var countIndex by rememberSaveable { mutableIntStateOf(0) }
    var refImage by remember { mutableStateOf<File?>(null) }
    var genState by remember { mutableStateOf<GenState>(GenState.Idle) }
    var showModelPicker by remember { mutableStateOf(false) }
    var pickedCat by remember { mutableStateOf("") }

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

    val scope = rememberCoroutineScope()

    // 二次编辑协调逻辑：载入原图作参考图 + 载入提示词 → 返回生成页现场微调
    val startEdit: (String, File) -> Unit = { editPrompt, imageFile ->
        prompt = editPrompt.take(MAX_PROMPT)
        styleIndex = 0
        refImage = imageFile
        genState = GenState.Idle
        currentTab = 0
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
            refImage = f
            if (f == null) genState = GenState.Failed("读取参考图失败")
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
                    refImage = refImage,
                    onPickRef = { picker.launch("image/*") },
                    onRemoveRef = { refImage = null },
                    modelName = selectedModel,
                    genState = genState,
                    onGenerate = {
                        if (activeProvider == null || baseUrl.isBlank() || apiKey.isBlank() || selectedModel.isBlank()) {
                            genState = GenState.Failed("请先在「我的」中配置供应商并选择模型")
                            return@GeneratePage
                        }
                        val userPrompt = prompt.trim().ifEmpty { SAMPLE_PROMPTS.first() }
                        val fullPrompt = userPrompt + STYLE_OPTIONS[styleIndex].second
                        val (qualityParam, steps) = qualityFor(qualityIndex)
                        genState = GenState.Loading
                        // 前台服务保活：切后台/锁屏不中断生成
                        KeepAliveService.start(context)
                        scope.launch {
                            val ref = refImage?.let { f ->
                                val mime = when (f.extension.lowercase()) {
                                    "webp" -> "image/webp"
                                    "jpg", "jpeg" -> "image/jpeg"
                                    "gif" -> "image/gif"
                                    else -> "image/png"
                                }
                                RefImage(f.readBytes(), mime)
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
                                    refImage = ref,
                                ),
                            ).onSuccess { outcome ->
                                val saved = mutableListOf<String>()
                                val now = System.currentTimeMillis()
                                val refName = ref?.let { store.saveRef(it.bytes, it.mime) }
                                outcome.images.forEachIndexed { idx, bytes ->
                                    store.save(bytes)?.let { name ->
                                        prefs.appendImage(fullPrompt, now + idx, name, refName)
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
                                genState = GenState.Failed(e.message ?: "生成失败")
                            }
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

        FloatingNav(
            selected = currentTab,
            onSelected = { currentTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 10.dp)
                .padding(bottom = 12.dp),
        )
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

    // 模型快捷选择面板
    if (showModelPicker) {        ModelPickerDialog(
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
                currentTab = 2
            },
            onDismiss = { showModelPicker = false },
        )
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
    refImage: File?,
    onPickRef: () -> Unit,
    onRemoveRef: () -> Unit,
    modelName: String,
    genState: GenState,
    onGenerate: () -> Unit,
    onModelClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = 76.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Header()
        Spacer(Modifier.height(8.dp))
        PromptCard(prompt = prompt, onPromptChange = onPromptChange)
        Spacer(Modifier.height(8.dp))
        RefImageRow(refImage = refImage, onPick = onPickRef, onRemove = onRemoveRef)
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
        GenerateButton(
            loading = genState is GenState.Loading,
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
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
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
private fun PromptCard(prompt: String, onPromptChange: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .glassCard(RoundedCornerShape(12.dp))
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
        }
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Palette.InputBg)
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
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

// ============ 参考图 ============

@Composable
private fun RefImageRow(refImage: File?, onPick: () -> Unit, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .glassCard(RoundedCornerShape(12.dp))
            .padding(start = 10.dp, end = 10.dp)
            .clickable(enabled = refImage == null, onClick = onPick),
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
        Spacer(Modifier.weight(1f))
        if (refImage == null) {
            Text(
                "选择图片 ＞",
                fontSize = 10.sp,
                color = Palette.InkMid,
            )
        } else {
            AsyncImage(
                model = refImage,
                contentDescription = "参考图预览",
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8E6F0))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "移除参考图",
                    tint = Palette.InkMid,
                    modifier = Modifier.size(10.dp),
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
            .glassCard(RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        // 模型（点击跳转「我的」）
        Row(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
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
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSel) Color.White else Color.Transparent)
                    .border(
                        width = if (isSel) 1.dp else 0.dp,
                        color = Palette.Purple,
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
    Row(
        Modifier
            .width(100.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Palette.InputBg),
    ) {
        options.forEachIndexed { i, o ->
            val isSel = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSel) Color.White else Color.Transparent)
                    .border(
                        width = if (isSel) 1.dp else 0.dp,
                        color = Palette.Purple,
                        shape = RoundedCornerShape(6.dp),
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

// 比例行：分段滑动选择器（线性滑行动画）
@Composable
private fun RatioRow(selected: Int, onSelect: (Int) -> Unit) {
    val ratios = RATIO_OPTIONS
    val indicatorX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        indicatorX.snapTo(selected * with(density) { (154.dp / ratios.size).toPx() })
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
                .clip(RoundedCornerShape(6.dp))
                .background(Palette.InputBg),
        ) {
            val seg = maxWidth / ratios.size
            val segPx = with(density) { seg.toPx() }

            Box(
                Modifier
                    .offset { IntOffset(indicatorX.value.roundToInt(), 0) }
                    .size(seg, 22.dp)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White)
                    .border(1.dp, Palette.Purple, RoundedCornerShape(4.dp)),
            )

            Row(Modifier.fillMaxSize()) {
                ratios.forEachIndexed { i, r ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                onSelect(i)
                                scope.launch {
                                    indicatorX.animateTo(i * segPx, tween(180, easing = LinearEasing))
                                }
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
            .background(Palette.ButtonBlue, RoundedCornerShape(10.dp))
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

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .glassCard(RoundedCornerShape(23.dp)),
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
    val file = File(context.cacheDir, "ref_${System.currentTimeMillis()}.$ext")
    val input = context.contentResolver.openInputStream(uri) ?: return null
    input.use { ins ->
        file.outputStream().use { outs -> ins.copyTo(outs) }
    }
    file
} catch (e: Exception) {
    null
}
