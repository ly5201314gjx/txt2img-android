package com.example.txt2img.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.txt2img.ui.theme.Palette

/**
 * 液态玻璃（Liquid Glass）卡片修饰器。
 * 高透乳白底（默认 85% 透明度）+ 1dp 高光微边框渐变（左上白 → 右下浅灰紫）+ 超轻微弥散阴影。
 */
fun Modifier.glassCard(
    shape: Shape,
    alpha: Float = 0.85f,
): Modifier = this
    .shadow(
        elevation = 12.dp,
        shape = shape,
        clip = false,
        ambientColor = Color(0x0C0A1E1A),
        spotColor = Color(0x0C0A1E1A),
    )
    .background(Color.White.copy(alpha = alpha), shape)
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.50f),
                Color.White.copy(alpha = 0.32f),
                Color(0xFFE2E1EE).copy(alpha = 0.20f),
            ),
        ),
        shape = shape,
    )

/**
 * 屏幕背景：极浅灰紫渐变 + 弥散冷紫光斑。
 */
@Composable
fun ScreenBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Palette.BgTop, Palette.BgBottom)))
            .drawSoftBlobs(),
    ) {
        content()
    }
}

private fun Modifier.drawSoftBlobs(): Modifier = this.drawBehind {
    val softLavender = Color(0xFFE7E3FB).copy(alpha = 0.90f)
    val coolLilac = Color(0xFFF0EDFC).copy(alpha = 0.85f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(softLavender, Color.Transparent),
            center = center,
            radius = size.width * 0.55f,
        ),
        radius = size.width * 0.55f,
        center = center.copy(x = size.width * 0.92f, y = size.height * 0.16f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(coolLilac, Color.Transparent),
            center = center,
            radius = size.width * 0.6f,
        ),
        radius = size.width * 0.6f,
        center = center.copy(x = size.width * 0.06f, y = size.height * 0.58f),
    )
}
