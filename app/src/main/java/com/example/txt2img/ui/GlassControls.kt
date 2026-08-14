package com.example.txt2img.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import com.example.txt2img.ui.theme.Palette
import com.liquidglass.ui.LocalGlassConfig
import com.liquidglass.ui.animation.DampedDragAnimation
import com.liquidglass.ui.animation.InteractiveHighlight
import com.liquidglass.ui.modifier.isLiquidGlassEnabled
import com.liquidglass.ui.modifier.liquidGlassBackground
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 液态玻璃分段选择器（复刻底部导航 FloatingBottomBar 的交互与动效）：
 *
 * - 玻璃胶囊底座：实时模糊背景层（vibrancy + blur + lens + 高光 + 投影）
 * - 可拖拽指示滑块：DampedDragAnimation 阻尼拖拽、松手吸附、速度形变
 * - 按压高光 InteractiveHighlight 跟随指示器位置
 * - 尺寸与回调完全兼容原 MiniSegmented / RatioRow
 *
 * @param options 选项文本
 * @param selectedIndex 当前选中下标
 * @param onSelect 切换回调
 * @param height 组件高度（与原来一致）
 */
@Composable
fun GlassDragSelector(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 22.dp,
    textSize: TextUnit = 8.sp,
    containerWidth: Dp = 100.dp,
    activeColor: Color = Palette.Purple,
) {
    val config = LocalGlassConfig.current
    val density = LocalDensity.current
    val animationScope = rememberCoroutineScope()
    val glassActive = isLiquidGlassEnabled() && config.glassLevel >= 1

    var totalWidthPx by remember { mutableFloatStateOf(0f) }
    var optWidthPx by remember { mutableFloatStateOf(0f) }

    val damped = remember(animationScope, options.size, density) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(options.size - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = 1.35f,
            canDrag = { true },
            onDragStarted = {},
            onDragStopped = {
                val target = targetValue.fastRoundToInt().fastCoerceIn(0, options.size - 1)
                animateToValue(target.toFloat())
                if (target != selectedIndex) {
                    onSelect(target)
                }
            },
            onDrag = { _, dragAmount ->
                if (optWidthPx > 0f) {
                    updateValue(
                        (targetValue + dragAmount.x / optWidthPx)
                            .fastCoerceIn(0f, (options.size - 1).toFloat()),
                    )
                }
            },
        )
    }

    LaunchedEffect(selectedIndex, damped) {
        snapshotFlow { selectedIndex }.collectLatest { index ->
            damped.animateToValue(index.toFloat())
        }
    }

    // 无玻璃环境：普通半透明胶囊，保留拖拽与点击
    val capsuleBase = if (glassActive) {
        Modifier
            .liquidGlassBackground(RoundedCornerShape(50), highlight = false)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(50))
    } else {
        Modifier.glassField(RoundedCornerShape(50))
    }

    Box(
        modifier = modifier
            .width(containerWidth)
            .height(height)
            .then(capsuleBase)
            .onGloballyPositioned { coords ->
                totalWidthPx = coords.size.width.toFloat()
                optWidthPx = totalWidthPx / options.size
            }
            // 拖拽手势：整块区域可拖（观察式，不消费事件，不干扰点击）
            .then(damped.modifier),
        contentAlignment = Alignment.Center,
    ) {
        // 选项格子：点击选中（唯一点击来源）
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, o ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            onSelect(i)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        o,
                        fontSize = textSize,
                        color = if (i == selectedIndex) activeColor else Palette.InkMid,
                        fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // 指示滑块：纯绘制（不拦截触摸），位置/缩放/形变全部由 damped 驱动
        if (optWidthPx > 0f) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = damped.value * optWidthPx
                    }
                    .width(with(density) { optWidthPx.toDp() })
                    .fillMaxHeight()
                    .padding(1.dp)
                    .then(
                        if (glassActive) {
                            Modifier
                                .liquidGlassBackground(RoundedCornerShape(20), highlight = false)
                                .background(
                                    Color.White.copy(alpha = 0.60f),
                                    RoundedCornerShape(20),
                                )
                        } else {
                            Modifier.glassChip(RoundedCornerShape(20))
                        }
                    )
                    .graphicsLayer {
                        val press = damped.pressProgress
                        val s = 1f + (1.35f - 1f) * press
                        scaleX = s
                        scaleY = s
                        val velocity = damped.velocity / 10f
                        scaleX /= 1f - (velocity * 0.6f).fastCoerceIn(-0.15f, 0.15f)
                        scaleY *= 1f - (velocity * 0.2f).fastCoerceIn(-0.15f, 0.15f)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    options[selectedIndex],
                    fontSize = textSize,
                    fontWeight = FontWeight.Bold,
                    color = activeColor,
                )
            }
        }
    }
}

/**
 * 玻璃按压胶囊（真玻璃版）：实时模糊背景层 + 按压缩放回弹。
 * 用于风格 chips、随机灵感、添加参考图等小型可点控件。
 */
@Composable
fun Modifier.realGlassChip(shape: RoundedCornerShape): Modifier {
    val glassActive = isLiquidGlassEnabled() &&
        LocalGlassConfig.current.glassLevel >= 1
    val base = if (glassActive) {
        this.liquidGlassBackground(shape, highlight = false)
    } else {
        this.glassChip(shape)
    }
    return base.glassPressable()
}

/**
 * 玻璃输入容器（真玻璃版）：实时模糊背景层，无按压动效（适合输入框）。
 */
@Composable
fun Modifier.realGlassField(shape: RoundedCornerShape): Modifier {
    val glassActive = isLiquidGlassEnabled() &&
        LocalGlassConfig.current.glassLevel >= 1
    return if (glassActive) {
        this.liquidGlassBackground(shape, highlight = false)
    } else {
        this.glassField(shape)
    }
}

/**
 * 玻璃按压状态行（真玻璃版）。
 */
@Composable
fun Modifier.realGlassRow(shape: RoundedCornerShape): Modifier {
    val glassActive = isLiquidGlassEnabled() &&
        LocalGlassConfig.current.glassLevel >= 1
    val base = if (glassActive) {
        this.liquidGlassBackground(shape, highlight = false)
    } else {
        this.glassField(shape)
    }
    return base.glassPressable()
}

/**
 * 玻璃功能卡片（真玻璃版）：实时模糊背景层，用于 Tab1 的功能模块卡片。
 * 保持 glassCard 的阴影与描边观感。
 */
@Composable
fun Modifier.realGlassCard(shape: RoundedCornerShape): Modifier {
    val glassActive = isLiquidGlassEnabled() &&
        LocalGlassConfig.current.glassLevel >= 1
    return if (glassActive) {
        this
            .liquidGlassBackground(shape, highlight = false)
            .background(Color.White.copy(alpha = 0.12f), shape)
            .drawGlassTopSheen()
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
    } else {
        this.glassCard(shape)
    }
}

/**
 * 玻璃顶部反光光晕：上缘一道白→透明渐变，经典的"玻璃边光"。
 */
private fun Modifier.drawGlassTopSheen(): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f),
                Color.Transparent,
            ),
            startY = 0f,
            endY = size.height * 0.40f,
        ),
    )
}

/**
 * 液态玻璃弹窗：
 * - Android 12+：窗口级实时模糊（背景内容真模糊）+ 半透明遮罩
 * - 弹簧入场动画（缩放 + 淡入）
 * - 面板本身由各弹窗使用 glassCard / realGlassCard 绘制
 */
@Composable
fun GlassDialog(
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        // 窗口级实时模糊（Android 12+）
        val view = LocalView.current
        LaunchedEffect(Unit) {
            val window = (view as? androidx.compose.ui.window.DialogWindowProvider)?.window
            if (window != null) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        window.setBackgroundBlurRadius(48)
                    }
                    window.setDimAmount(0.30f)
                }
            }
        }
        // 入场动画：弹簧缩放 + 淡入
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val scale by animateFloatAsState(
            targetValue = if (shown) 1f else 0.92f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
            label = "glassDialogScale",
        )
        val dialogAlpha by animateFloatAsState(
            targetValue = if (shown) 1f else 0f,
            animationSpec = tween(220),
            label = "glassDialogAlpha",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = dialogAlpha
                },
        ) {
            content()
        }
    }
}
