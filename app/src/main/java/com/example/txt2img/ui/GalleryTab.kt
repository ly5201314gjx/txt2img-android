package com.example.txt2img.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.txt2img.data.AppPrefs
import com.example.txt2img.data.ImageStore
import com.example.txt2img.data.MediaSaver
import com.example.txt2img.data.PrefsJson
import com.example.txt2img.ui.theme.Palette
import com.liquidglass.ui.topbar.GlassMediumFlexibleTopAppBar
import java.io.File
import kotlinx.coroutines.launch
import org.json.JSONArray

private data class ImageEntry(
    val prompt: String,
    val time: Long,
    val file: String,
    val refFile: String?,
    val cat: String,
    val durationMs: Long,
    val ratio: String,
    val type: String,
)

private fun parseEntries(json: String): List<ImageEntry> = try {
    val arr = JSONArray(json)
    val list = mutableListOf<ImageEntry>()
    for (i in 0 until arr.length()) {
        val o = arr.optJSONObject(i) ?: continue
        list.add(
            ImageEntry(
                prompt = o.optString("prompt", ""),
                time = o.optLong("time", 0L),
                file = o.optString("file", ""),
                refFile = o.optString("ref", "").ifEmpty { null },
                cat = o.optString("cat", ""),
                durationMs = o.optLong("dur", 0L),
                ratio = o.optString("ratio", ""),
                type = o.optString("type", ""),
            ),
        )
    }
    list
} catch (e: Exception) {
    emptyList()
}

/**
 * 作品页：分类筛选 + 网格 + 长按归类/删除 + 详情面板。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryTab(
    imagesJson: String,
    catsJson: String,
    store: ImageStore,
    prefs: AppPrefs,
    onEdit: (String, File) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val entries = remember(imagesJson) { parseEntries(imagesJson) }
    val cats = remember(catsJson) { PrefsJson.parseCats(catsJson) }

    var filter by remember { mutableStateOf<String?>(null) } // null = 全部
    var viewer by remember { mutableStateOf<ImageEntry?>(null) }
    var fullView by remember { mutableStateOf<String?>(null) } // 全屏查看的文件名
    var menuTarget by remember { mutableStateOf<ImageEntry?>(null) }
    var pendingMove by remember { mutableStateOf<ImageEntry?>(null) }
    var pendingDelete by remember { mutableStateOf<ImageEntry?>(null) }
    var showPrompt by remember { mutableStateOf<String?>(null) }
    var multiMode by remember { mutableStateOf(false) }
    var selectedSet by remember { mutableStateOf<Set<String>>(emptySet()) }
    var multiMove by remember { mutableStateOf(false) }
    var multiDelete by remember { mutableStateOf(false) }
    var catMenu by remember { mutableStateOf<String?>(null) } // null 未打开；"" = 全部
    var showReorder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var showNewCat by remember { mutableStateOf(false) }
    var deleteCat by remember { mutableStateOf<String?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<Pair<File, String>?>(null) }

    val allPos by prefs.allPos.collectAsState(initial = 0)

    val toast: (String) -> Unit = { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pendingSave?.let { (f, n) ->
                toast(if (MediaSaver.save(context, f, n)) "已保存到相册" else "保存失败")
            }
        } else {
            toast("未授权存储权限，无法保存到相册")
        }
        pendingSave = null
        permissionRequested = false
    }

    fun saveToGallery(file: File, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            toast(if (MediaSaver.save(context, file, name)) "已保存到相册" else "保存失败")
            return
        }
        if (context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            toast(if (MediaSaver.save(context, file, name)) "已保存到相册" else "保存失败")
        } else {
            permissionRequested = true
            pendingSave = Pair(file, name)
            permLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val shown = if (filter == null) entries else entries.filter { it.cat == filter }

    // 分类展示顺序（"全部"参与排序，位置持久化）
    val chipOrder = remember(cats, allPos) {
        cats.take(allPos.coerceIn(0, cats.size)) + listOf("全部") + cats.drop(allPos.coerceIn(0, cats.size))
    }

    fun exitMulti() {
        multiMode = false
        selectedSet = emptySet()
    }

    Box(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(top = 64.dp)
            .padding(bottom = if (multiMode) 150.dp else 76.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            // 分类筛选条（支持长按操作：移动位置 / 重命名 / 删除）
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                chipOrder.forEach { name ->
                    val isAll = name == "全部"
                    FilterChip(
                        text = name,
                        selected = if (isAll) filter == null else filter == name,
                        onClick = {
                            if (isAll) filter = null else filter = name
                        },
                        onLongClick = { catMenu = if (isAll) "" else name },
                    )
                }
                Box(
                    Modifier
                        .glassChip(RoundedCornerShape(10.dp))
                        .clickable { showNewCat = true }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "＋ 新建",
                        fontSize = 9.sp,
                        color = Palette.InkMid,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            if (shown.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                Box(
                    Modifier
                        .size(56.dp)
                        .realGlassCard(RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PhotoLibrary,
                        contentDescription = null,
                        tint = Palette.InkLight,
                        modifier = Modifier.size(28.dp),
                    )
                }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (filter == null) "暂无作品" else "该分类暂无作品",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.InkStrong,
                    )
                    Text(
                        if (filter == null) "生成的图片会自动保存在这里" else "可以长按其他作品移动到该分类",
                        fontSize = 10.sp,
                        color = Palette.InkLight,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shown, key = { it.file }) { e ->
                        val file = store.fileFor(e.file)
                        if (file.exists()) {
                            val sel = e.file in selectedSet
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .realGlassCard(RoundedCornerShape(10.dp))
                                    .then(
                                        if (sel) {
                                            Modifier.border(
                                                1.5.dp,
                                                Palette.Purple,
                                                RoundedCornerShape(10.dp),
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .glassPressable()
                                    .combinedClickable(
                                        onClick = {
                                            if (multiMode) {
                                                selectedSet = if (sel) selectedSet - e.file else selectedSet + e.file
                                            } else {
                                                viewer = e
                                            }
                                        },
                                        onLongClick = {
                                            if (!multiMode) menuTarget = e
                                        },
                                    ),
                            ) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = e.prompt,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                )
                                if (e.type == "reverse") {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(Palette.Purple.copy(alpha = 0.85f))
                                            .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(99.dp))
                                            .padding(horizontal = 7.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            "反推",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                        )
                                    }
                                }
                                if (multiMode) {
                                    Box(
                                        Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (sel) Palette.Purple else Color.White.copy(alpha = 0.85f),
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (sel) Color.White.copy(alpha = 0.5f) else Palette.InkLight,
                                                shape = RoundedCornerShape(6.dp),
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (sel) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp),
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

        // 多选操作栏
        if (multiMode) {
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 88.dp)
                    .fillMaxWidth()
                    .height(40.dp)
                    .realGlassCard(RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "已选 ${selectedSet.size} 张",
                    fontSize = 9.sp,
                    color = Palette.InkMid,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .glassChip(RoundedCornerShape(8.dp))
                        .clickable(enabled = selectedSet.isNotEmpty()) { multiMove = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "移动分类",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedSet.isNotEmpty()) Palette.InkStrong else Palette.InkLight,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .glassChip(RoundedCornerShape(8.dp))
                        .background(Color(0xCCFBEDED))
                        .clickable(enabled = selectedSet.isNotEmpty()) { multiDelete = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "删除",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedSet.isNotEmpty()) ErrorRed else Palette.InkLight,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .glassChip(RoundedCornerShape(8.dp))
                        .clickable(onClick = ::exitMulti)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        "取消",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.InkStrong,
                    )
                }
            }
        }
    }

    // 长按菜单（多选 / 归类 / 删除）
    menuTarget?.let { e ->
        ActionMenuDialog(
            title = "长按操作",
            options = listOf("多选", "移动到分类", "删除"),
            onPick = { i ->
                menuTarget = null
                when (i) {
                    0 -> {
                        multiMode = true
                        selectedSet = setOf(e.file)
                    }
                    1 -> pendingMove = e
                    2 -> pendingDelete = e
                }
            },
            onDismiss = { menuTarget = null },
        )
    }

    // 单张移动到分类
    pendingMove?.let { e ->
        CategoryPickerDialog(
            categories = cats,
            current = e.cat,
            onPick = { c ->
                scope.launch { prefs.setImageCategory(e.file, c) }
                pendingMove = null
                toast(if (c.isEmpty()) "已移出分类" else "已归类到「$c」")
            },
            onDismiss = { pendingMove = null },
        )
    }

    // 批量移动到分类
    if (multiMove) {
        CategoryPickerDialog(
            categories = cats,
            current = "",
            onPick = { c ->
                val targets = selectedSet
                scope.launch {
                    targets.forEach { prefs.setImageCategory(it, c) }
                }
                exitMulti()
                toast(if (c.isEmpty()) "已移出分类" else "已归类到「$c」")
            },
            onDismiss = { multiMove = false },
        )
    }

    // 批量删除确认
    if (multiDelete) {
        ConfirmDialog(
            title = "删除选中的 ${selectedSet.size} 张作品？",
            message = "将同时删除对应的参考图，且不可恢复。",
            confirmText = "删除",
            onConfirm = {
                val targets = selectedSet
                scope.launch {
                    targets.forEach { name ->
                        prefs.removeImage(name)
                        store.delete(name)
                    }
                }
                exitMulti()
                toast("已删除 ${targets.size} 张")
            },
            onDismiss = { multiDelete = false },
        )
    }

    // 详情面板
    viewer?.let { v ->
        val ref = v.refFile?.let { store.fileFor(it) }?.takeIf { it.exists() }
        ImageDetailDialog(
            file = store.fileFor(v.file),
            prompt = v.prompt,
            time = v.time,
            durationMs = v.durationMs,
            ratio = v.ratio,
            refFile = ref,
            onDismiss = { viewer = null },
            onEdit = {
                viewer = null
                onEdit(v.prompt, store.fileFor(v.file))
            },
            onShowPrompt = { p ->
                showPrompt = p
            },
            onViewImage = {
                fullView = v.file
            },
            onCopyPrompt = { p ->
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("prompt", p))
                toast("提示词已复制")
            },
            onSaveImage = {
                val name = "IMG_${v.time}.png"
                saveToGallery(store.fileFor(v.file), name)
            },
            onSaveRef = {
                if (ref != null) {
                    val name = "REF_${v.time}.png"
                    saveToGallery(ref, name)
                }
            },
            onDelete = {
                viewer = null
                pendingDelete = v
            },
        )
    }

    // 完整提示词查看
    showPrompt?.let { p ->
        PromptViewDialog(
            prompt = p,
            onCopy = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("prompt", p))
                toast("提示词已复制")
            },
            onDismiss = { showPrompt = null },
        )
    }

    // 全屏图片查看
    fullView?.let { name ->
        FullscreenImageViewer(
            file = store.fileFor(name),
            onDismiss = { fullView = null },
        )
    }

    // 分类长按操作菜单（移动位置 / 重命名 / 删除）
    catMenu?.let { target ->
        val isAll = target.isEmpty()
        ActionMenuDialog(
            title = if (isAll) "分类「全部」" else "分类「$target」",
            options = if (isAll) listOf("移动位置") else listOf("移动位置", "重命名", "删除"),
            onPick = { i ->
                catMenu = null
                when {
                    i == 0 -> showReorder = true
                    !isAll && i == 1 -> renameTarget = target
                    !isAll && i == 2 -> deleteCat = target
                }
            },
            onDismiss = { catMenu = null },
        )
    }

    // 分类排序（含"全部"）
    if (showReorder) {
        ReorderDialog(
            title = "调整分类顺序",
            items = chipOrder,
            onConfirm = { newOrder ->
                val newAllPos = newOrder.indexOf("全部").coerceAtLeast(0)
                val newCats = newOrder.filter { it != "全部" }
                scope.launch {
                    prefs.saveCats(newCats)
                    prefs.saveAllPos(newAllPos)
                }
                showReorder = false
                toast("顺序已保存")
            },
            onDismiss = { showReorder = false },
        )
    }

    // 分类重命名
    renameTarget?.let { old ->
        TextInputDialog(
            title = "重命名分类",
            placeholder = "输入新名称（≤16 字）",
            initial = old,
            onConfirm = { name ->
                if (name.isEmpty() || name == old || name in cats) {
                    toast("名称无效或已存在")
                } else {
                    scope.launch {
                        prefs.saveCats(cats.map { if (it == old) name else it })
                        entries.filter { it.cat == old }.forEach { prefs.setImageCategory(it.file, name) }
                    }
                    if (filter == old) filter = name
                    toast("已重命名为「$name」")
                }
                renameTarget = null
            },
            onDismiss = { renameTarget = null },
        )
    }

    // 删除作品确认
    pendingDelete?.let { v ->
        ConfirmDialog(
            title = "删除这张作品？",
            message = "将同时删除图片与对应的参考图，且不可恢复。",
            confirmText = "删除",
            onConfirm = {
                scope.launch {
                    prefs.removeImage(v.file)
                    store.delete(v.file)
                    v.refFile?.let { store.delete(it) }
                }
                pendingDelete = null
                toast("已删除")
            },
            onDismiss = { pendingDelete = null },
        )
    }

    // 新建分类
    if (showNewCat) {
        TextInputDialog(
            title = "新建分类",
            placeholder = "分类名称（≤16 字）",
            onConfirm = { name ->
                if (name.isEmpty() || name in cats) {
                    toast("名称无效或已存在")
                } else {
                    scope.launch { prefs.saveCats(cats + name) }
                    toast("已创建「$name」")
                    filter = name
                }
                showNewCat = false
            },
            onDismiss = { showNewCat = false },
        )
    }

    // 删除分类确认
    deleteCat?.let { c ->
        ConfirmDialog(
            title = "删除分类「$c」？",
            message = "该分类下的作品会变为未分类，图片本身不会删除。",
            confirmText = "删除",
            onConfirm = {
                scope.launch {
                    prefs.saveCats(cats.filter { it != c })
                    entries.filter { it.cat == c }.forEach { prefs.setImageCategory(it.file, "") }
                }
                if (filter == c) filter = null
                deleteCat = null
                toast("已删除分类")
            },
            onDismiss = { deleteCat = null },
        )
    }
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val base = Modifier
        .clip(RoundedCornerShape(10.dp))
        .background(
            if (selected) {
                Palette.Purple.copy(alpha = 0.88f)
            } else {
                Color.White.copy(alpha = 0.45f)
            }
        )
        .border(
            width = 1.dp,
            color = if (selected) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.35f),
            shape = RoundedCornerShape(10.dp),
        )
        .glassPressable()
        .padding(horizontal = 10.dp, vertical = 4.dp)
    val clickMod = if (onLongClick != null) {
        @OptIn(ExperimentalFoundationApi::class)
        base.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        base.clickable(onClick = onClick)
    }
    Box(clickMod) {
        Text(
            text,
            fontSize = 9.sp,
            color = if (selected) Color.White else Palette.InkMid,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val ErrorRed = Color(0xFFC2473F)
