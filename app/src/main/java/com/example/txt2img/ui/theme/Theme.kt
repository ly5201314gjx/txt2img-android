package com.example.txt2img.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 全局色调（Liquid Glassmorphism · 暖杏色系）
object Palette {
    // 背景（暖奶油 → 暖米杏）
    val BgTop = Color(0xFFFBF6EF)
    val BgBottom = Color(0xFFF3E8DA)
    // 文本（暖棕深墨，暖色系更有质感）
    val InkTitle = Color(0xFF2C2219)      // 页面主标题
    val InkStrong = Color(0xFF3D3126)     // 模块标题/选项
    val InkMid = Color(0xFF857262)        // 普通描述
    val InkLight = Color(0xFFAB9C8C)      // 辅助/计数
    // 品牌色（杏橙）
    val Purple = Color(0xFFE07B39)        // 主品牌（杏橙）
    val BrandDeep = Color(0xFFB95F26)     // 深杏
    val BrandSoft = Color(0xFFFFB27A)     // 浅杏（渐变高光）
    val Amber = Color(0xFFC9A227)         // 金色点缀（状态/徽章）
    val CreditBg = Color(0xFFFFE8D5)      // 杏橙浅底
    val Avatar = Color(0xFF7A6A5C)
    val ButtonBlue = Color(0xFFE07B39)    // 主按钮（品牌杏橙）
    // 容器（暖调玻璃）
    val InputBg = Color(0x52FFFFFF)      // 玻璃化：半透明白
    val Divider = Color(0x26FFFFFF)      // 玻璃化：半透明白分隔
    val GlassBorderDim = Color(0xFFE9DDCE)
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
