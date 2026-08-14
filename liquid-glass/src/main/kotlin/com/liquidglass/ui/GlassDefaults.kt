package com.liquidglass.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 玻璃颜色统一处理。
 */
object GlassDefaults {

    /** 默认模糊透明度 */
    const val DefaultBlurAlpha = 0.36f

    /** 厚模糊透明度 */
    const val ThickBlurAlpha = 0.72f

    /** 全透明 */
    const val TransparentAlpha = 0f

    /**
     * 根据是否启用模糊决定容器颜色。
     * @param noBlurColor 未开启模糊时使用的颜色
     * @param blurAlpha 开启模糊时应用的透明度
     */
    @Composable
    fun glassColor(noBlurColor: Color, blurAlpha: Float): Color {
        return if (LocalGlassConfig.current.enableBlur) {
            noBlurColor.copy(alpha = blurAlpha)
        } else {
            noBlurColor
        }
    }
}
