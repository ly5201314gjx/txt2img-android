package com.example.txt2img.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.txt2img.ui.theme.Palette
import kotlinx.coroutines.launch

/**
 * 玻璃卡片修饰器（半透明假玻璃，原项目同款模式）。
 *
 * 注意：页面内容层内的卡片不做 drawBackdrop（避免采样自身所在层）。
 * 真液态玻璃仅用于浮层组件（底部导航、顶栏）。
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
 * 玻璃输入容器（输入框、分段选择器底槽等）。
 * 半透明白 + 细高光描边，适合内容层内的控件。
 */
@Composable
fun Modifier.glassField(shape: Shape): Modifier = this
    .background(Color.White.copy(alpha = 0.28f), shape)
    .border(1.dp, Color.White.copy(alpha = 0.35f), shape)

/**
 * 玻璃小胶囊（chips、药丸按钮等）。
 * 透明度更高 + 高光描边。
 */
@Composable
fun Modifier.glassChip(shape: Shape): Modifier = this
    .background(Color.White.copy(alpha = 0.55f), shape)
    .border(1.dp, Color.White.copy(alpha = 0.60f), shape)

/**
 * 玻璃按压动效：按下缩小 0.96，松手弹簧回弹。
 *
 * 观察式实现（不消费事件）：组件自身的 clickable 完全不受影响，
 * 尺寸不变，仅叠加 graphicsLayer 变换。
 * 用法：`Modifier.glassPressable().clickable { ... }`
 */
@Composable
fun Modifier.glassPressable(): Modifier {
    val scope = rememberCoroutineScope()
    val scale = remember { Animatable(1f) }
    return this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = 1f - (1f - scale.value) * 2.5f
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(false)
                scope.launch {
                    scale.animateTo(0.96f, spring(dampingRatio = 0.55f, stiffness = 800f))
                }
                waitForUpOrCancellation()
                scope.launch {
                    scale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 600f))
                }
            }
        }
}

/**
 * 玻璃分隔线：半透明白细线。
 */
fun Modifier.glassDivider(): Modifier = this
    .fillMaxWidth()
    .height(0.5.dp)
    .background(Color.White.copy(alpha = 0.20f))

/**
 * 虚线玻璃卡片：虚线描边 + 微透明白底，用于「添加」类入口，区别于实线卡片。
 */
@Composable
fun Modifier.dashedGlassCard(
    shape: RoundedCornerShape,
    cornerRadius: Dp = 14.dp,
    dashColor: Color = Color(0xFFD9B98C),
): Modifier {
    val strokeWidth = 1.dp
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = 0.20f), shape)
        .drawBehind {
            val sw = strokeWidth.value * density
            drawRoundRect(
                color = dashColor,
                topLeft = Offset(sw, sw),
                size = Size(size.width - 2f * sw, size.height - 2f * sw),
                cornerRadius = CornerRadius(cornerRadius.value * density),
                style = Stroke(
                    width = sw,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                ),
            )
        }
        .glassPressable()
}

/**
 * 屏幕背景：极浅灰紫渐变 + 弥散冷紫光斑。
 * 作为液态玻璃的采样背景层（放入 LiquidGlassHost 的 background 槽）。
 */
@Composable
fun ScreenBackdrop(content: @Composable BoxScope.() -> Unit = {}) {
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
    val warmApricot = Color(0xFFFFE3C2).copy(alpha = 0.90f)
    val warmPeach = Color(0xFFFFF0DC).copy(alpha = 0.85f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(warmApricot, Color.Transparent),
            center = center,
            radius = size.width * 0.55f,
        ),
        radius = size.width * 0.55f,
        center = center.copy(x = size.width * 0.92f, y = size.height * 0.16f),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(warmPeach, Color.Transparent),
            center = center,
            radius = size.width * 0.6f,
        ),
        radius = size.width * 0.6f,
        center = center.copy(x = size.width * 0.06f, y = size.height * 0.58f),
    )
}
