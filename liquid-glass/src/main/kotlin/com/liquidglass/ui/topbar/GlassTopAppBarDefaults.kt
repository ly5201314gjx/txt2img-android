package com.liquidglass.ui.topbar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.liquidglass.ui.GlassDefaults
import com.liquidglass.ui.LocalGlassConfig

/**
 * 玻璃顶栏默认配置。
 */
object GlassTopAppBarDefaults {

    @Composable
    fun glassColors(): TopAppBarColors {

        val containerBaseColor = MaterialTheme.colorScheme.surface
        val containerColor = GlassDefaults.glassColor(
            noBlurColor = containerBaseColor,
            blurAlpha = GlassDefaults.TransparentAlpha
        )

        val scrolledBaseColor = MaterialTheme.colorScheme.surfaceContainer
        val scrolledContainerColor = if (LocalGlassConfig.current.enableBlur) {
            scrolledBaseColor.copy(alpha = GlassDefaults.TransparentAlpha)
        } else {
            scrolledBaseColor
        }

        return TopAppBarDefaults.topAppBarColors(
            containerColor = applyTopBarOpacity(containerColor),
            scrolledContainerColor = applyTopBarOpacity(scrolledContainerColor)
        )
    }

    @Composable
    fun containerColor(): Color {
        val baseColor = MaterialTheme.colorScheme.surface
        val glassColor = GlassDefaults.glassColor(
            noBlurColor = baseColor,
            blurAlpha = GlassDefaults.TransparentAlpha
        )
        return applyTopBarOpacity(glassColor)
    }

    @Composable
    fun scrolledContainerColor(): Color {
        val baseColor = MaterialTheme.colorScheme.surfaceContainer
        val glassColor = GlassDefaults.glassColor(
            noBlurColor = baseColor,
            blurAlpha = GlassDefaults.TransparentAlpha
        )
        return applyTopBarOpacity(glassColor)
    }

    /** 顶栏按钮/胶囊的容器色（半透明玻璃感） */
    @Composable
    fun controlContainerColor(): Color {
        val baseColor = GlassDefaults.glassColor(
            noBlurColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            blurAlpha = GlassDefaults.DefaultBlurAlpha
        )
        return applyTopBarOpacity(baseColor)
    }

    @Composable
    private fun applyTopBarOpacity(color: Color): Color {
        val opacity = (LocalGlassConfig.current.topBarOpacity.coerceIn(0, 100)) / 100f
        return color.copy(alpha = (color.alpha * opacity).coerceIn(0f, 1f))
    }
}
