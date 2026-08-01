package com.example.txt2img.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.txt2img.data.CurrentSelection
import com.example.txt2img.data.ProviderConfig
import com.example.txt2img.ui.theme.Palette
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 玻璃质感图片预览弹窗（生成结果 / 作品查看共用）。
 */
@Composable
fun GlassImageDialog(
    file: File,
    prompt: String,
    time: Long = 0L,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    categories: List<String> = emptyList(),
    pickedCat: String = "",
    onPickCategory: (String) -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            AsyncImage(
                model = file,
                contentDescription = prompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                prompt,
                fontSize = 11.sp,
                color = Palette.InkStrong,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (time > 0L) {
                Spacer(Modifier.height(4.dp))
                Text(
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(time)),
                    fontSize = 8.sp,
                    color = Palette.InkLight,
                )
            }
            if (categories.isNotEmpty()) {
                CategoryBar(categories, pickedCat, onPickCategory)
            }
            Spacer(Modifier.height(12.dp))
            if (onEdit != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.InputBg)
                            .clickable(onClick = onEdit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "继续微调",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.InkStrong,
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.ButtonBlue)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "完成",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.ButtonBlue)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "完成",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/**
 * 多图结果弹窗（生成数量 > 1 时使用）。
 */
@Composable
fun MultiImageDialog(
    images: List<File>,
    prompt: String,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
    categories: List<String> = emptyList(),
    pickedCat: String = "",
    onPickCategory: (String) -> Unit = {},
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .glassCard(RoundedCornerShape(16.dp))
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "已生成 ${images.size} 张",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                prompt,
                fontSize = 9.sp,
                color = Palette.InkLight,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            if (categories.isNotEmpty()) {
                CategoryBar(categories, pickedCat, onPickCategory)
            }
            Spacer(Modifier.height(6.dp))
            images.forEach { file ->
                AsyncImage(
                    model = file,
                    contentDescription = prompt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            if (onEdit != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.InputBg)
                            .clickable(onClick = onEdit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "继续微调",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.InkStrong,
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.ButtonBlue)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "完成",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.ButtonBlue)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "完成",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

// 归类横条：未分类 + 各分类
@Composable
private fun CategoryBar(categories: List<String>, picked: String, onPick: (String) -> Unit) {
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "归类",
            fontSize = 9.sp,
            color = Palette.InkMid,
        )
        CatChip("未分类", picked.isEmpty(), onClick = { onPick("") })
        categories.forEach { c ->
            CatChip(c, picked == c, onClick = { onPick(c) })
        }
    }
}

@Composable
private fun CatChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Palette.Purple else Palette.InputBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            text,
            fontSize = 8.sp,
            color = if (selected) Color.White else Palette.InkMid,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 作品详情面板：二次编辑 / 下载到相册 / 复制提示词 / 保存参考图 / 删除。
 */
@Composable
fun ImageDetailDialog(
    file: File,
    prompt: String,
    time: Long,
    durationMs: Long = 0L,
    ratio: String = "",
    refFile: File?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCopyPrompt: (String) -> Unit,
    onSaveImage: () -> Unit,
    onSaveRef: () -> Unit,
    onDelete: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            AsyncImage(
                model = file,
                contentDescription = prompt,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp)),
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    prompt,
                    fontSize = 11.sp,
                    color = Palette.InkStrong,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.Purple)
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "二次编辑",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.InputBg)
                        .clickable { onCopyPrompt(prompt) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        "复制提示词",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.InkStrong,
                    )
                }
            }
            if (time > 0L) {
                Spacer(Modifier.height(6.dp))
                Text(
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(time)),
                    fontSize = 8.sp,
                    color = Palette.InkLight,
                )
            }
            if (durationMs > 0L || ratio.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                val dur = if (durationMs > 0L) "耗时 ${String.format(Locale.getDefault(), "%.1f", durationMs / 1000.0)}s" else ""
                val parts = listOf(dur, if (ratio.isNotEmpty()) "比例 $ratio" else "")
                    .filter { it.isNotEmpty() }
                Text(
                    parts.joinToString(" · "),
                    fontSize = 8.sp,
                    color = Palette.InkLight,
                )
            }
            if (refFile != null && refFile.exists()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "参考图",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.InkStrong,
                    )
                    Spacer(Modifier.width(8.dp))
                    AsyncImage(
                        model = refFile,
                        contentDescription = "参考图",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.InputBg)
                            .clickable(onClick = onSaveRef)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Text(
                            "保存参考图",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Palette.InkStrong,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.ButtonBlue)
                        .clickable(onClick = onSaveImage),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "下载图片",
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
                        "删除",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorRed,
                    )
                }
            }
        }
    }
}

/**
 * 轻量确认弹窗（删除确认等）。
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                fontSize = 11.sp,
                color = Palette.InkMid,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.InputBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "取消",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.InkStrong,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFBEDED))
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        confirmText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ErrorRed,
                    )
                }
            }
        }
    }
}

/**
 * 提示引导弹窗（蓝色主按钮，用于后台保护等引导）。
 */
@Composable
fun TipDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                fontSize = 11.sp,
                color = Palette.InkMid,
                lineHeight = 16.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.InputBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "暂不",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.InkStrong,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.ButtonBlue)
                        .clickable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        confirmText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/**
 * 长按操作菜单。
 */
@Composable
fun ActionMenuDialog(
    title: String,
    options: List<String>,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(6.dp))
            options.forEachIndexed { i, o ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPick(i) }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        o,
                        fontSize = 11.sp,
                        color = Palette.InkStrong,
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "取消",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.InkStrong,
                )
            }
        }
    }
}

/**
 * 分类选择弹窗（移动到分类）。
 */
@Composable
fun CategoryPickerDialog(
    categories: List<String>,
    current: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 480.dp)
                .glassCard(RoundedCornerShape(16.dp))
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "移动到分类",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(6.dp))
            PickerRow("未分类", current.isEmpty(), onClick = { onPick("") })
            categories.forEach { c ->
                PickerRow(c, c == current, onClick = { onPick(c) })
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "取消",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.InkStrong,
                )
            }
        }
    }
}

@Composable
private fun PickerRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (selected) Palette.Purple else Color.Transparent),
            )
            if (!selected) {
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
            text,
            fontSize = 11.sp,
            color = if (selected) Palette.Purple else Palette.InkStrong,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/**
 * 文本输入弹窗（新建分类等）。
 */
@Composable
fun TextInputDialog(
    title: String,
    placeholder: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .glassCard(RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty()) {
                    Text(
                        placeholder,
                        fontSize = 11.sp,
                        color = Palette.InkLight,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { if (it.length <= 16) text = it },
                    modifier = Modifier.fillMaxSize(),
                    textStyle = TextStyle(fontSize = 11.sp, color = Palette.InkStrong),
                    singleLine = true,
                    cursorBrush = SolidColor(Palette.Purple),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.InputBg)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "取消",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.InkStrong,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.ButtonBlue)
                        .clickable {
                            if (text.trim().isNotEmpty()) onConfirm(text.trim())
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "确定",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/**
 * 关于弹窗：免责声明与联系方式。
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 540.dp)
                .glassCard(RoundedCornerShape(16.dp))
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                "关于文字生图",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkTitle,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "版本 v1.4",
                fontSize = 9.sp,
                color = Palette.InkLight,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "免责声明",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "本应用是作者在空闲时间随手开发的个人项目，纯属兴趣之作，不用于任何商业用途，不以盈利为目的，仅作为文字生图工具使用。",
                fontSize = 11.sp,
                color = Palette.InkMid,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "使用过程中如有任何问题、建议，或希望增加的功能，欢迎随时提出，作者会认真听取并持续改进。",
                fontSize = 11.sp,
                color = Palette.InkMid,
                lineHeight = 17.sp,
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "联系邮箱",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkStrong,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .clickable {
                        runCatching {
                            val intent = Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse("mailto:liuyu34work@163.com"),
                            )
                            context.startActivity(intent)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    "liuyu34work@163.com",
                    fontSize = 11.sp,
                    color = Palette.ButtonBlue,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "关闭",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.InkStrong,
                )
            }
        }
    }
}

/**
 * 模型快捷选择面板（底部弹出）：生成模型 / Agent 扶正模型通用。
 */
@Composable
fun ModelPickerDialog(
    providers: List<ProviderConfig>,
    current: CurrentSelection,
    title: String = "选择模型",
    subtitle: String = "点击即切换，无需保存",
    onPick: (String, String) -> Unit,
    onGoConfig: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .padding(bottom = 24.dp)
                    .glassCard(RoundedCornerShape(20.dp))
                    .padding(14.dp)
                    .heightIn(max = 480.dp),
            ) {
                Text(
                    title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Palette.InkTitle,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    fontSize = 9.sp,
                    color = Palette.InkLight,
                )
                Spacer(Modifier.height(10.dp))
                if (providers.isEmpty()) {
                    Text(
                        "尚未配置模型供应商，请先前往「我的」添加",
                        fontSize = 10.sp,
                        color = Palette.InkMid,
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Palette.ButtonBlue)
                            .clickable(onClick = onGoConfig),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "去配置",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .heightIn(max = 380.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        providers.forEach { p ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    p.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Palette.InkStrong,
                                )
                                if (current.providerId == p.id) {
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(Palette.CreditBg)
                                            .padding(horizontal = 6.dp, vertical = 1.dp),
                                    ) {
                                        Text(
                                            "使用中",
                                            fontSize = 8.sp,
                                            color = Palette.Purple,
                                        )
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Text(
                                    p.url.ifEmpty { "未配置" },
                                    fontSize = 8.sp,
                                    color = Palette.InkLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (p.models.isEmpty()) {
                                Text(
                                    "暂无模型，请在「我的」中获取",
                                    fontSize = 8.sp,
                                    color = Palette.InkLight,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                            p.models.forEach { m ->
                                val sel = current.providerId == p.id && current.model == m
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(32.dp)
                                        .clickable { onPick(p.id, m) },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                                        Box(
                                            Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(if (sel) Palette.Purple else Color.Transparent),
                                        )
                                        if (!sel) {
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
                                        color = if (sel) Palette.Purple else Palette.InkStrong,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
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

/**
 * Agent 扶正结果预览：原提示词 vs 优化后。
 */
@Composable
fun OptimizePreviewDialog(
    original: String,
    optimized: String,
    onApply: () -> Unit,
    onCancel: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .glassCard(RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Text(
                "提示词扶正完成",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkTitle,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "原提示词",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.InkLight,
            )
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .padding(8.dp),
            ) {
                Text(
                    original,
                    fontSize = 9.sp,
                    color = Palette.InkMid,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "优化后",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Palette.Purple,
            )
            Spacer(Modifier.height(3.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    optimized,
                    fontSize = 10.sp,
                    color = Palette.InkStrong,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.InputBg)
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "暂不应用",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.InkStrong,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.ButtonBlue)
                        .clickable(onClick = onApply),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "应用并生成",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/**
 * 图片反推结果弹窗。
 */
@Composable
fun ReverseResultDialog(
    sourceFile: File,
    category: String,
    prompt: String,
    onCopy: () -> Unit,
    onApplyBox: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .glassCard(RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = sourceFile,
                    contentDescription = "反推源图",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "图片反推完成",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.InkTitle,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "类型：$category",
                        fontSize = 9.sp,
                        color = Palette.Purple,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    prompt,
                    fontSize = 10.sp,
                    color = Palette.InkStrong,
                    lineHeight = 15.sp,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.InputBg)
                        .clickable(onClick = onCopy),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "复制",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.InkStrong,
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Palette.InputBg)
                        .clickable(onClick = onApplyBox),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "用到生成框",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Palette.InkStrong,
                    )
                }
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
                        "保存到作品",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Palette.InputBg)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "关闭",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Palette.InkStrong,
                )
            }
        }
    }
}

private val ErrorRed = Color(0xFFC2473F)
