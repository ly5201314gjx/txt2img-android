package com.example.txt2img.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.txt2img.data.AppPrefs
import com.example.txt2img.data.PrefsJson
import com.example.txt2img.data.ProviderConfig
import com.example.txt2img.net.ImageClient
import com.example.txt2img.ui.theme.Palette
import com.example.txt2img.util.SystemUtils
import com.liquidglass.ui.topbar.GlassMediumFlexibleTopAppBar
import kotlinx.coroutines.launch

private val ErrorRed = Color(0xFFC2473F)

private data class ProviderDraft(val name: String, val url: String, val key: String)

/**
 * 我的页：多供应商折叠管理（三角展开）、关于弹窗。
 */
@Composable
fun MyTab(prefs: AppPrefs, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val providersJson by prefs.providersJson.collectAsState(initial = "[]")
    val currentJson by prefs.currentJson.collectAsState(initial = "{}")
    var providers by remember(providersJson) { mutableStateOf(PrefsJson.parseProviders(providersJson)) }
    val current = remember(currentJson) { PrefsJson.parseCurrent(currentJson) }

    var expandedId by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<MutableMap<String, ProviderDraft>>(mutableMapOf()) }
    var loadingId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var showKeyId by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showExplainPicker by remember { mutableStateOf(false) }
    var explaining by remember { mutableStateOf(false) }
    var explainResult by remember { mutableStateOf<String?>(null) }
    var explainFail by remember { mutableStateOf<String?>(null) }

    val lastError by prefs.lastError.collectAsState(initial = "")
    val currentErrorMsg = remember(lastError) {
        try {
            org.json.JSONObject(lastError).optString("msg", lastError)
        } catch (e: Exception) {
            lastError
        }
    }
    val failLimit by prefs.failLimit.collectAsState(initial = 3)
    var failLimitText by remember { mutableStateOf("") }
    LaunchedEffect(failLimit) {
        if (failLimitText.isEmpty()) failLimitText = failLimit.toString()
    }

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()

    fun persist(newList: List<ProviderConfig>) {
        providers = newList
        scope.launch { prefs.saveProviders(newList) }
    }

    fun fetchModelsFor(providerId: String, draft: ProviderDraft) {
        if (draft.url.isBlank() || draft.key.isBlank()) {
            status = "请先填写接口地址与 API Key"
            return
        }
        loadingId = providerId
        status = "正在获取模型…"
        scope.launch {
            ImageClient.fetchModels(draft.url, draft.key)
                .onSuccess { list ->
                    val updated = providers.map { p ->
                        if (p.id == providerId) {
                            p.copy(
                                models = list,
                                selectedModel = if (list.contains(p.selectedModel)) p.selectedModel else list.firstOrNull().orEmpty(),
                            )
                        } else p
                    }
                    persist(updated)
                    status = if (list.isEmpty()) "未获取到模型" else "获取到 ${list.size} 个模型"
                }
                .onFailure { status = "获取失败：${it.message}" }
            loadingId = null
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp)
            .padding(top = 64.dp)
            .padding(bottom = 76.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        providers.forEach { p ->
            ProviderCard(
                provider = p,
                expanded = expandedId == p.id,
                draft = drafts[p.id] ?: ProviderDraft(p.name, p.url, p.key),
                isCurrent = current.providerId == p.id,
                loading = loadingId == p.id,
                showKey = showKeyId == p.id,
                onToggle = { expandedId = if (expandedId == p.id) null else p.id },
                onDraftChange = { d ->
                    drafts = (drafts + (p.id to d)).toMutableMap()
                },
                onToggleKey = { showKeyId = if (showKeyId == p.id) null else p.id },
                onFetch = { fetchModelsFor(p.id, drafts[p.id] ?: ProviderDraft(p.name, p.url, p.key)) },
                onSelectModel = { m ->
                    persist(providers.map { if (it.id == p.id) it.copy(selectedModel = m) else it })
                    scope.launch { prefs.saveCurrent(p.id, m) }
                    toast("已切换到 $m")
                },
                onToggleShown = { m ->
                    persist(
                        providers.map {
                            if (it.id == p.id) {
                                it.copy(
                                    shownModels = if (m in it.shownModels) {
                                        it.shownModels - m
                                    } else {
                                        it.shownModels + m
                                    },
                                )
                            } else it
                        },
                    )
                },
                onSave = {
                    val d = drafts[p.id] ?: ProviderDraft(p.name, p.url, p.key)
                    persist(providers.map { if (it.id == p.id) it.copy(name = d.name, url = d.url, key = d.key) else it })
                    status = "已保存，自动同步模型…"
                    fetchModelsFor(p.id, d)
                },
                onDelete = { deleteTarget = p.id },
            )
            Spacer(Modifier.height(8.dp))
        }

        // 添加供应商（虚线玻璃入口）
        Box(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .dashedGlassCard(RoundedCornerShape(14.dp))
                .clickable {
                    val id = "p${System.currentTimeMillis()}"
                    val name = "供应商 ${providers.size + 1}"
                    persist(providers + ProviderConfig(id, name, "", "", emptyList(), emptyList(), ""))
                    drafts = (drafts + (id to ProviderDraft(name, "", ""))).toMutableMap()
                    expandedId = id
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "＋ 添加供应商",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Palette.InkMid,
            )
        }

        status?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                fontSize = 9.sp,
                color = if (it.startsWith("获取失败") || it.startsWith("请先")) ErrorRed else Palette.InkMid,
                lineHeight = 12.sp,
            )
        }

        Spacer(Modifier.height(8.dp))

        // 后台运行保护（电池优化豁免）
        var batteryOk by remember { mutableStateOf(!SystemUtils.isBatteryOptimized(context)) }
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .glassCard(RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp)
                .glassPressable()
                .clickable {
                    SystemUtils.requestBatteryExemption(context)
                    batteryOk = !SystemUtils.isBatteryOptimized(context)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(Color.White.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PowerSettingsNew,
                    contentDescription = null,
                    tint = Palette.InkMid,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "后台运行保护",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.InkTitle,
                )
                Text(
                    "允许忽略电池优化，后台生成不中断",
                    fontSize = 9.sp,
                    color = Palette.InkLight,
                )
            }
            Text(
                if (batteryOk) "已开启" else "未开启",
                fontSize = 10.sp,
                color = if (batteryOk) Palette.Amber else Palette.InkMid,
                fontWeight = if (batteryOk) FontWeight.SemiBold else FontWeight.Normal,
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Palette.InkLight,
                modifier = Modifier.size(14.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // 生成容错设置（连续失败停止次数）
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(12.dp))
                .padding(12.dp),
        ) {
            Text(
                "生成容错",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "请求上游连续失败达到设定次数后自动停止生成，防止无效消耗",
                fontSize = 8.sp,
                color = Palette.InkLight,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "连续失败停止次数",
                    fontSize = 10.sp,
                    color = Palette.InkStrong,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .width(56.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    BasicTextField(
                        value = failLimitText,
                        onValueChange = { input ->
                            if (input.length <= 2 && input.all { it.isDigit() }) {
                                failLimitText = input
                                input.toIntOrNull()?.let { v ->
                                    scope.launch { prefs.saveFailLimit(v) }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        textStyle = TextStyle(fontSize = 13.sp, color = Palette.InkStrong),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        ),
                        cursorBrush = SolidColor(Palette.Purple),
                        decorationBox = { innerTextField ->
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                innerTextField()
                            }
                        },
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "次",
                    fontSize = 10.sp,
                    color = Palette.InkMid,
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 最近一次请求失败信息（点击查看详情，关闭后清除）
        if (lastError.isNotEmpty()) {
            val errTime = try {
                org.json.JSONObject(lastError).optLong("time", 0L)
            } catch (e: Exception) {
                0L
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .glassCard(RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp)
                    .glassPressable()
                    .clickable { showErrorDialog = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(28.dp)
                        .background(Color(0xFFFBEDED).copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Error,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "最近一次请求失败",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed,
                    )
                    if (errTime > 0L) {
                        Text(
                            java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(errTime)),
                            fontSize = 9.sp,
                            color = Palette.InkLight,
                        )
                    }
                }
                Text(
                    "查看详情",
                    fontSize = 10.sp,
                    color = ErrorRed,
                )
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Palette.InkLight,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        // 关于（品牌行）
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .glassCard(RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp)
                .glassPressable()
                .clickable { showAbout = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(28.dp)
                    .background(
                        Brush.linearGradient(listOf(Palette.BrandSoft, Palette.Purple)),
                        RoundedCornerShape(9.dp),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Info,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "关于",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkTitle,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "免责声明 · 联系作者",
                fontSize = 10.sp,
                color = Palette.InkMid,
            )
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = Palette.InkLight,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    // 失败详情面板（关闭即清除）
    if (showErrorDialog) {
        ErrorDetailDialog(
            errorText = currentErrorMsg,
            explainResult = explainResult,
            explaining = explaining,
            onExplain = {
                if (providers.isEmpty()) {
                    toast("请先添加供应商")
                } else {
                    showExplainPicker = true
                }
            },
            onCopyError = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("error", currentErrorMsg))
                toast("报错已复制")
            },
            onCopyExplain = {
                explainResult?.let { r ->
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("explain", r))
                    toast("分析结果已复制")
                }
            },
            onDismiss = {
                scope.launch { prefs.clearLastError() }
                showErrorDialog = false
                explainResult = null
                explainFail = null
            },
        )
    }

    // AI 解析报错：选择模型
    if (showExplainPicker) {
        ModelPickerDialog(
            providers = providers,
            current = current,
            title = "选择解析模型",
            subtitle = "用选中的模型分析报错原因与解决办法",
            onPick = { pid, m ->
                showExplainPicker = false
                val p = providers.find { it.id == pid }
                if (p != null) {
                    explaining = true
                    explainResult = null
                    scope.launch {
                        ImageClient.explainError(p.url, p.key, m, currentErrorMsg)
                            .onSuccess { r ->
                                explaining = false
                                explainResult = r
                            }
                            .onFailure { e ->
                                explaining = false
                                explainFail = e.message?.take(120)
                                toast("解析失败：${e.message?.take(80)}")
                            }
                    }
                }
            },
            onGoConfig = { showExplainPicker = false },
            onDismiss = { showExplainPicker = false },
        )
    }

    deleteTarget?.let { id ->
        ConfirmDialog(
            title = "删除供应商？",
            message = "将删除该供应商的名称、地址、Key 与模型列表；若正在使用将自动切换。",
            confirmText = "删除",
            onConfirm = {
                val newList = providers.filter { it.id != id }
                persist(newList)
                if (current.providerId == id) {
                    val first = newList.firstOrNull()
                    scope.launch {
                        prefs.saveCurrent(first?.id.orEmpty(), first?.selectedModel.orEmpty())
                    }
                }
                deleteTarget = null
                toast("已删除")
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

// ============ 供应商折叠卡片 ============

@Composable
private fun ProviderCard(
    provider: ProviderConfig,
    expanded: Boolean,
    draft: ProviderDraft,
    isCurrent: Boolean,
    loading: Boolean,
    showKey: Boolean,
    onToggle: () -> Unit,
    onDraftChange: (ProviderDraft) -> Unit,
    onToggleKey: () -> Unit,
    onFetch: () -> Unit,
    onSelectModel: (String) -> Unit,
    onToggleShown: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .realGlassCard(RoundedCornerShape(14.dp)),
    ) {
        // 头部：渐变首字徽标 + 名称 + 使用中（琥珀徽章）+ 旋转箭头
        val arrowRotate by animateFloatAsState(
            targetValue = if (expanded) 90f else 0f,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
            label = "arrowRotate",
        )
        Row(
            Modifier
                .fillMaxWidth()
                .height(58.dp)
                .glassPressable()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 首字渐变徽标
            Box(
                Modifier
                    .size(34.dp)
                    .background(
                        Brush.linearGradient(listOf(Palette.BrandSoft, Palette.Purple)),
                        RoundedCornerShape(11.dp),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    provider.name.take(1).ifEmpty { "供" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    provider.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.InkTitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (provider.url.isNotBlank()) {
                        if (provider.shownModels.isNotEmpty()) {
                            provider.url + " · 已展示 ${provider.shownModels.size} 个模型"
                        } else {
                            provider.url
                        }
                    } else {
                        "未配置接口地址"
                    },
                    fontSize = 10.sp,
                    color = Palette.InkLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCurrent) {
                Spacer(Modifier.width(8.dp))
                // 琥珀金「使用中」徽章
                Box(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color(0xFFFCF1D5))
                        .border(1.dp, Color(0xFFE2BE62), RoundedCornerShape(99.dp))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                ) {
                    Text(
                        "使用中",
                        fontSize = 8.sp,
                        color = Color(0xFF9A7400),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = null,
                tint = Palette.InkMid,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = arrowRotate },
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) + fadeIn(tween(200)),
            exit = shrinkVertically(spring(dampingRatio = 0.8f, stiffness = 500f)) + fadeOut(tween(150)),
        ) {
            Column(Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp)) {
                FieldLabel("名称")
                SettingsField(draft.name, { onDraftChange(draft.copy(name = it)) }, "供应商名称")
                Spacer(Modifier.height(8.dp))

                FieldLabel("接口地址")
                SettingsField(draft.url, { onDraftChange(draft.copy(url = it)) }, "https://api.example.com/v1")
                Spacer(Modifier.height(8.dp))

                FieldLabel("API Key")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = draft.key,
                        onValueChange = { onDraftChange(draft.copy(key = it)) },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontSize = 11.sp, color = Palette.InkStrong),
                        singleLine = true,
                        cursorBrush = SolidColor(Palette.Purple),
                        visualTransformation = if (showKey) {
                            androidx.compose.ui.text.input.VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                    )
                    Icon(
                        imageVector = if (showKey) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (showKey) "隐藏 Key" else "显示 Key",
                        tint = Palette.InkLight,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onToggleKey)
                            .padding(6.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Palette.ButtonBlue.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .glassPressable()
                        .clickable(enabled = !loading, onClick = onFetch),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (loading) "获取中…" else "获取模型",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }

                if (provider.models.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "共 ${provider.models.size} 个模型 · 勾选展示 · 设为生图模型",
                        fontSize = 8.sp,
                        color = Palette.InkLight,
                    )
                    Spacer(Modifier.height(2.dp))
                    provider.models.forEach { m ->
                        val isShown = m in provider.shownModels
                        val isCurrentModel = isCurrent && provider.selectedModel == m
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(34.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 勾选（展示子集）
                            Box(
                                Modifier
                                    .size(18.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isShown) Palette.Purple else Color.Transparent)
                                    .border(
                                        width = if (isShown) 0.dp else 1.dp,
                                        color = Palette.InkLight,
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                    .clickable { onToggleShown(m) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isShown) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                m,
                                fontSize = 10.sp,
                                color = if (isCurrentModel) Palette.Purple else Palette.InkStrong,
                                fontWeight = if (isCurrentModel) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (isCurrentModel) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Color(0xFFFCF1D5))
                                        .border(1.dp, Color(0xFFE2BE62), RoundedCornerShape(99.dp))
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        "使用中",
                                        fontSize = 8.sp,
                                        color = Color(0xFF9A7400),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            } else {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(Color.White.copy(alpha = 0.45f))
                                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(99.dp))
                                        .glassPressable()
                                        .clickable { onSelectModel(m) }
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        "设为当前",
                                        fontSize = 8.sp,
                                        color = Palette.InkMid,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Palette.ButtonBlue.copy(alpha = 0.92f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .glassPressable()
                            .clickable(onClick = onSave),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "保存配置",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xCCFBEDED), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                            .glassPressable()
                            .clickable(onClick = onDelete),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "删除供应商",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ErrorRed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 9.sp, color = Palette.InkMid)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun SettingsField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.32f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontSize = 12.sp,
                color = Palette.InkStrong,
                lineHeight = 16.sp,
            ),
            singleLine = true,
            cursorBrush = SolidColor(Palette.Purple),
            decorationBox = { innerTextField ->
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 12.sp,
                            color = Palette.InkLight.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}
