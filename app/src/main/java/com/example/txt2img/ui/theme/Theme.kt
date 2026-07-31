package com.example.txt2img.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 全局色调（Liquid Glassmorphism · 极浅灰紫）
object Palette {
    // 背景
    val BgTop = Color(0xFFF6F5FA)
    val BgBottom = Color(0xFFEFEFF8)
    // 文本
    val InkTitle = Color(0xFF1E1B2E)      // 页面主标题
    val InkStrong = Color(0xFF2D293E)     // 模块标题/选项
    val InkMid = Color(0xFF6E6A80)        // 普通描述
    val InkLight = Color(0xFF9E9AA8)      // 辅助/计数
    // 品牌色
    val Purple = Color(0xFF5B48EF)
    val CreditBg = Color(0xFFEEECFB)
    val Avatar = Color(0xFF555166)
    val ButtonBlue = Color(0xFF007AFF)
    // 容器
    val InputBg = Color(0xFFF1EFF8)
    val Divider = Color(0xFFF0EFF7)
    val GlassBorderDim = Color(0xFFE2E1EE)
}

private val LightColors = lightColorScheme(
    primary = Palette.Purple,
    onPrimary = Color.White,
    background = Palette.BgTop,
    onBackground = Palette.InkTitle,
    surface = Color.White,
    onSurface = Palette.InkTitle,
)

@Composable
fun Txt2ImgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
