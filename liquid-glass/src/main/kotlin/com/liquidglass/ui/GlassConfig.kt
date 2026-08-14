package com.liquidglass.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import dev.chrisbanes.haze.HazeState

/**
 * 顶栏按钮样式。
 */
enum class TopBarButtonStyle(val storageValue: String) {
    /** 无容器，裸图标 */
    Plain("plain"),

    /** 淡色调容器 */
    Tonal("tonal"),

    /** 描边容器 */
    Outlined("outlined"),

    /** 半透明容器（Haze 模糊，Android 13 以下可用） */
    SemiTransparent("glass"),

    /** 液态玻璃（Backdrop 实时渲染，仅 Android 13+） */
    LiquidGlass("liquid");

    companion object {
        fun fromStorage(value: String?): TopBarButtonStyle =
            entries.firstOrNull { it.storageValue == value } ?: Tonal
    }
}

/**
 * 液态玻璃系统全局配置。
 * 通过 [LocalGlassConfig] 提供，所有组件自动感知。
 */
data class GlassConfig(
    /** 是否启用 Haze 背景模糊（Android 13 以下液态玻璃的兜底） */
    val enableBlur: Boolean = true,

    /** 是否启用渐进式模糊（顶栏顶部 100% → 底部 0%） */
    val enableProgressiveBlur: Boolean = false,

    /** 顶栏 Haze 模糊半径（dp） */
    val topBarBlurRadius: Int = 24,

    /** 顶栏 Haze 模糊透明度（0-100） */
    val topBarBlurAlpha: Int = 73,

    /** 顶栏容器不透明度（0-100） */
    val topBarOpacity: Int = 100,

    /** 顶栏按钮样式 */
    val topBarButtonStyle: TopBarButtonStyle = TopBarButtonStyle.LiquidGlass,

    /** 是否把多个顶栏按钮合并为一个胶囊 */
    val mergeTopBarActions: Boolean = false,

    /** 是否使用可折叠的 MediumFlexibleTopAppBar */
    val useFlexibleTopAppBar: Boolean = true,

    /** 底部栏液态玻璃模糊半径 */
    val bottomBarBlurRadius: Float = 24f,

    /** 底部栏不透明度（0-100） */
    val bottomBarBlurAlpha: Int = 80,

    /** 底部栏镜头效果半径 */
    val bottomBarLensRadius: Dp = 24.dp,

    /** 玻璃卡片容器不透明度（0-100） */
    val containerOpacity: Int = 100,

    /** 底部栏指示图标强调色；Color.Unspecified 时回退到 MaterialTheme primary */
    val accentColor: Color = Color.Unspecified,

    /**
     * 保守模式：仅使用基础模糊（RenderEffect），禁用 AGSL 着色器（vibrancy/lens）
     * 与按压高光动画。兼容性最好，适合 GPU 较弱或出现渲染崩溃的设备。
     */
    val glassConservative: Boolean = false,

    /**
     * 是否把内容层（UI）也纳入玻璃采样源。
     * 为 false 时仅采样背景层，规避内容层录制导致的递归渲染风险，兼容性最佳。
     */
    val glassSampleContent: Boolean = true,

    /**
     * 玻璃渲染级别（自动分级测试用）：
     * 0 = 不渲染玻璃（仅挂载层）
     * 1 = 基础模糊（RenderEffect，无 AGSL）
     * 2 = 完整特效（vibrancy/lens/按压高光）
     */
    val glassLevel: Int = 2,
) {
    companion object {
        val Default = GlassConfig()
    }
}

/** 全局玻璃配置 */
val LocalGlassConfig = compositionLocalOf { GlassConfig.Default }

/** 宿主提供的 Backdrop（背景层 + 内容层合并）。 */
val LocalGlassBackdrop = compositionLocalOf<Backdrop?> { null }

/**
 * 宿主提供的「仅背景层」Backdrop。
 * 供内容层内的组件安全采样（背景层纹理不含内容层自身，无自采样递归风险）。
 */
val LocalGlassBackgroundBackdrop = compositionLocalOf<Backdrop?> { null }

/** 宿主提供的 HazeState；由 [LiquidGlassHost] 提供，enableBlur=false 时为 null */
val LocalGlassHazeState = compositionLocalOf<HazeState?> { null }
