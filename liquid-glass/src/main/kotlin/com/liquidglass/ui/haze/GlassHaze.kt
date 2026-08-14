package com.liquidglass.ui.haze

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import com.liquidglass.ui.LocalGlassConfig

/**
 * 把内容注册为 Haze 模糊来源（玻璃背后的画面）。
 * 仅在 [LocalGlassConfig.enableBlur] 时生效。
 */
@Composable
fun Modifier.glassHazeSource(state: HazeState): Modifier = this.then(
    if (LocalGlassConfig.current.enableBlur) Modifier.hazeSource(state) else Modifier
)

/**
 * 传统的背景模糊效果（非 Android 13+ 设备上液态玻璃的兜底方案）。
 *
 * @param containerColor 容器底色
 * @param blurRadius 模糊半径（dp）
 * @param blurAlpha 模糊层透明度（0-100）
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.glassHazeEffect(
    state: HazeState,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    blurRadius: Int = LocalGlassConfig.current.topBarBlurRadius,
    blurAlpha: Int = LocalGlassConfig.current.topBarBlurAlpha,
): Modifier {
    val config = LocalGlassConfig.current
    if (!config.enableBlur) return this

    val style = HazeStyle(
        blurRadius = blurRadius.dp,
        backgroundColor = containerColor,
        tint = HazeTint(
            containerColor.copy(alpha = blurAlpha / 100f)
        ),
    )

    return this.hazeEffect(
        state = state,
        style = style
    ) {
        progressive = if (config.enableProgressiveBlur) {
            HazeProgressive.verticalGradient(
                startIntensity = 1f,
                endIntensity = 0f
            )
        } else {
            null
        }
    }
}
