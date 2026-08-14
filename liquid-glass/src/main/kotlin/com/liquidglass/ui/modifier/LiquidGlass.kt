package com.liquidglass.ui.modifier

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.liquidglass.ui.LocalGlassBackdrop
import com.liquidglass.ui.LocalGlassBackgroundBackdrop
import com.liquidglass.ui.LocalGlassConfig
import com.liquidglass.ui.animation.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * 液态玻璃核心 Modifier（采样「背景层 + 内容层」）。
 * 仅限内容层之外的组件使用（顶栏/浮层）。
 *
 * @param shape 玻璃容器形状（决定模糊采样区域）。注意 lens 特效要求圆角形状。
 */
@Composable
fun Modifier.liquidGlass(shape: Shape): Modifier {
    val backdrop = LocalGlassBackdrop.current ?: return this
    return liquidGlassWith(backdrop, shape, highlight = true)
}

/**
 * 液态玻璃 Modifier（仅采样「背景层」）。
 * 内容层内的组件可安全使用：背景层纹理不含内容层自身，无自采样递归风险。
 *
 * @param highlight 是否启用按压高光着色器（AGSL）。大尺寸容器建议 false 以降低渲染开销。
 */
@Composable
fun Modifier.liquidGlassBackground(shape: Shape, highlight: Boolean = false): Modifier {
    val backdrop = LocalGlassBackgroundBackdrop.current ?: return this
    return liquidGlassWith(backdrop, shape, highlight)
}

@Composable
private fun Modifier.liquidGlassWith(
    backdrop: Backdrop,
    shape: Shape,
    highlight: Boolean = true,
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    val level = LocalGlassConfig.current.glassLevel
    if (level < 1) return this
    val containerColor = MaterialTheme.colorScheme.surface.copy(
        alpha = 0.5f
    )
    val shadowColor = Color.Black.copy(alpha = 0.04f)
    val conservative = level < 2
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = if (conservative || !highlight) {
        null
    } else {
        remember(animationScope) {
            runCatching { InteractiveHighlight(animationScope) }.getOrNull()
        }
    }
    val highlightModifier = interactiveHighlight?.let {
        it.modifier.then(it.gestureModifier)
    } ?: Modifier
    // lens/vibrancy 仅支持圆角类形状（RoundedRectangularShape / CornerBasedShape），
    // 其他形状（如 RectangleShape）会导致 UnsupportedOperationException
    val shapeSupportsSdf = shape is androidx.compose.foundation.shape.RoundedCornerShape ||
        shape is androidx.compose.foundation.shape.CornerBasedShape
    return runCatching {
        drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                if (!conservative && shapeSupportsSdf) {
                    vibrancy()
                }
                blur(12.dp.toPx())
                if (!conservative && shapeSupportsSdf) {
                    lens(24.dp.toPx(), 24.dp.toPx())
                }
            },
            highlight = { Highlight.Default },
            shadow = {
                Shadow(
                    radius = 12.dp,
                    color = shadowColor
                )
            },
            layerBlock = {
                val width = size.width
                val height = size.height
                if (width > 0f && height > 0f) {
                    val progress = interactiveHighlight?.pressProgress ?: 0f
                    val scale = 1f + 4.dp.toPx() / height * progress
                    val maxOffset = size.minDimension
                    val dragOffset = interactiveHighlight?.dragOffset ?: Offset.Zero
                    translationX = maxOffset * tanh(0.05f * dragOffset.x / maxOffset) * progress
                    translationY = maxOffset * tanh(0.05f * dragOffset.y / maxOffset) * progress
                    val maxDragScale = 4.dp.toPx() / height
                    val offsetAngle = atan2(dragOffset.y, dragOffset.x)
                    scaleX = scale + maxDragScale *
                            abs(cos(offsetAngle) * dragOffset.x / size.maxDimension) *
                            (width / height).coerceAtMost(1f) * progress
                    scaleY = scale + maxDragScale *
                            abs(sin(offsetAngle) * dragOffset.y / size.maxDimension) *
                            (height / width).coerceAtMost(1f) * progress
                }
            },
            onDrawSurface = {
                drawRect(containerColor)
            },
        )
            .then(highlightModifier)
    }.getOrDefault(this)
}

/**
 * 当前环境是否可启用液态玻璃。
 * 需在 [com.liquidglass.ui.LiquidGlassHost] 内部且 Android 13+。
 */
@Composable
fun isLiquidGlassEnabled(): Boolean =
    LocalGlassBackdrop.current != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
