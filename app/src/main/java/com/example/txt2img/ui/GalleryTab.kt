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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.io.File
import kotlinx.coroutines.launch
import org.json.JSONArray

private data class ImageEntry(
    val prompt: String,
    val time: Long,
    val file: String,
    val refFile: String?,
    val cat: String,
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
    var menuTarget by remember { mutableStateOf<ImageEntry?>(null) }
    var pendingMove by remember { mutableStateOf<ImageEntry?>(null) }
    var pendingDelete by remember { mutableStateOf<ImageEntry?>(null) }
    var showNewCat by remember { mutableStateOf(false) }
    var deleteCat by remember { mutableStateOf<String?>(null) }
    var permissionRequested by remember { mutableStateOf(false) }
    var pendingSave by remember { mutableStateOf<Pair<File, String>?>(null) }

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

    Column(
        modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
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
                    "作品",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.InkTitle,
                    letterSpacing = 0.2.sp,
                )
                Text(
                    "点击查看详情 · 长按归类 / 删除",
                    fontSize = 9.sp,
                    color = Palette.InkLight,
                )
            }
        }

        // 分类筛选条
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilterChip("全部", filter == null, onClick = { filter = null })
            cats.forEach { c ->
                FilterChip(
                    text = c,
                    selected = filter == c,
                    onClick = { filter = c },
                    onLongClick = { deleteCat = c },
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.7f))
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
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = null,
                    tint = Palette.InkLight,
                    modifier = Modifier.size(28.dp),
                )
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
                        AsyncImage(
                            model = file,
                            contentDescription = e.prompt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .combinedClickable(
                                    onClick = { viewer = e },
                                    onLongClick = { menuTarget = e },
                                ),
                        )
                    }
                }
            }
        }
    }

    // 长按菜单
    menuTarget?.let { e ->
        ActionMenuDialog(
            title = "长按操作",
            options = listOf("移动到分类", "删除"),
            onPick = { i ->
                menuTarget = null
                when (i) {
                    0 -> pendingMove = e
                    1 -> pendingDelete = e
                }
            },
            onDismiss = { menuTarget = null },
        )
    }

    // 移动到分类
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

    // 详情面板
    viewer?.let { v ->
        val ref = v.refFile?.let { store.fileFor(it) }?.takeIf { it.exists() }
        ImageDetailDialog(
            file = store.fileFor(v.file),
            prompt = v.prompt,
            time = v.time,
            refFile = ref,
            onDismiss = { viewer = null },
            onEdit = {
                viewer = null
                onEdit(v.prompt, store.fileFor(v.file))
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
        .background(if (selected) Palette.Purple else Color.White.copy(alpha = 0.7f))
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
