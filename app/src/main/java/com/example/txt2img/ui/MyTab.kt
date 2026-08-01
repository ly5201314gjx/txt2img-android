package com.example.txt2img.ui

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    val visionJson by prefs.visionJson.collectAsState(initial = "{}")
    var providers by remember(providersJson) { mutableStateOf(PrefsJson.parseProviders(providersJson)) }
    val current = remember(currentJson) { PrefsJson.parseCurrent(currentJson) }

    var expandedId by remember { mutableStateOf<String?>(null) }
    var drafts by remember { mutableStateOf<MutableMap<String, ProviderDraft>>(mutableMapOf()) }
    var loadingId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var showKeyId by remember { mutableStateOf<String?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

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
            .padding(bottom = 76.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(start = 2.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "我的",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.InkTitle,
                    letterSpacing = 0.2.sp,
                )
                Text(
                    "多供应商模型服务管理",
                    fontSize = 9.sp,
                    color = Palette.InkLight,
                )
            }
        }
        Spacer(Modifier.height(4.dp))

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
                    scope.launch {
                        prefs.saveCurrent(p.id, m)
                        // 协调性：此处选模型同样触发视觉能力测试并缓存
                        val key = "${p.id}|$m"
                        val cached = try {
                            org.json.JSONObject(visionJson).optString(key, "")
                        } catch (e: Exception) {
                            ""
                        }
                        if (cached.isEmpty() && p.url.isNotBlank() && p.key.isNotBlank()) {
                            val ok = ImageClient.testVision(p.url, p.key, m).isSuccess
                            prefs.saveVisionResult(key, ok)
                            toast(if (ok) "该模型支持图片识别，已启用反推" else "该模型不支持图片识别，反推不可用")
                        }
                    }
                    toast("已切换到 $m")
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

        // 添加供应商
        Box(
            Modifier
                .fillMaxWidth()
                .height(36.dp)
                .glassCard(RoundedCornerShape(12.dp))
                .clickable {
                    val id = "p${System.currentTimeMillis()}"
                    val name = "供应商 ${providers.size + 1}"
                    persist(providers + ProviderConfig(id, name, "", "", emptyList(), ""))
                    drafts = (drafts + (id to ProviderDraft(name, "", ""))).toMutableMap()
                    expandedId = id
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "＋ 添加供应商",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Palette.InkStrong,
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
                .height(44.dp)
                .glassCard(RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp)
                .clickable {
                    SystemUtils.requestBatteryExemption(context)
                    batteryOk = !SystemUtils.isBatteryOptimized(context)
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.PowerSettingsNew,
                contentDescription = null,
                tint = Palette.InkMid,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "后台运行保护",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.InkStrong,
                )
                Text(
                    "允许忽略电池优化，后台生成不中断",
                    fontSize = 8.sp,
                    color = Palette.InkLight,
                )
            }
            Text(
                if (batteryOk) "已开启 ＞" else "未开启 ＞",
                fontSize = 10.sp,
                color = if (batteryOk) Palette.Purple else Palette.InkMid,
                fontWeight = if (batteryOk) FontWeight.SemiBold else FontWeight.Normal,
            )
        }

        Spacer(Modifier.height(8.dp))

        // 关于
        Row(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .glassCard(RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp)
                .clickable { showAbout = true },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = Palette.InkMid,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "关于",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "免责声明 · 联系作者 ＞",
                fontSize = 10.sp,
                color = Palette.InkMid,
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
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
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glassCard(RoundedCornerShape(12.dp)),
    ) {
        // 头部：三角 + 名称 + 使用中
        Row(
            Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Palette.InkMid,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    provider.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.InkStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    provider.url.ifEmpty { "未配置接口地址" },
                    fontSize = 8.sp,
                    color = Palette.InkLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (isCurrent) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(Palette.CreditBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "使用中",
                        fontSize = 8.sp,
                        color = Palette.Purple,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        if (expanded) {
            Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
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
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.InputBg)
                        .padding(start = 10.dp, end = 4.dp),
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
                        .height(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.ButtonBlue)
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
                        "共 ${provider.models.size} 个模型 · 点击选用",
                        fontSize = 8.sp,
                        color = Palette.InkLight,
                    )
                    Spacer(Modifier.height(2.dp))
                    provider.models.forEach { m ->
                        val isSel = provider.selectedModel == m
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clickable { onSelectModel(m) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) Palette.Purple else Color.Transparent),
                                )
                                if (!isSel) {
                                    Box(
                                        Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, Palette.InkLight, CircleShape),
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                m,
                                fontSize = 10.sp,
                                color = if (isSel) Palette.Purple else Palette.InkStrong,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.ButtonBlue)
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
                            .background(Color(0xFFFBEDED))
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
    Spacer(Modifier.height(4.dp))
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
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Palette.InputBg)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 11.sp,
                color = Palette.InkLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(fontSize = 11.sp, color = Palette.InkStrong),
            singleLine = true,
            cursorBrush = SolidColor(Palette.Purple),
        )
    }
}
