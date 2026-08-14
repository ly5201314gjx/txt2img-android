package com.liquidglass.ui.topbar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liquidglass.ui.LocalGlassConfig
import com.liquidglass.ui.LocalGlassHazeState
import com.liquidglass.ui.button.GlassTopBarActionsRow
import com.liquidglass.ui.haze.glassHazeEffect
import com.liquidglass.ui.modifier.liquidGlass

private val FlexibleExpandedHeight = 152.dp
private val FlexibleCollapsedHeight = 64.dp

internal val GlassFlexibleExpandedHeight = FlexibleExpandedHeight
internal val GlassFlexibleCollapsedHeight = FlexibleCollapsedHeight

/**
 * 中等可折叠玻璃顶栏。
 *
 * 自实现折叠行为（不依赖 material3 实验性 API）：
 * - 滚动时容器色在 containerColor ↔ scrolledContainerColor 之间插值
 * - 标题随折叠进度缩放并上移，subtitle 淡出
 * - 支持 bottomContent（如搜索栏、TabRow）
 * - 标题切换有淡入淡出动画
 *
 * 使用方式见 [GlassTopAppBarDefaults.defaultScrollBehavior]。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GlassMediumFlexibleTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollBehavior: GlassTopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomContent: @Composable (ColumnScope.() -> Unit)? = null
) {

    val hazeState = LocalGlassHazeState.current
    val config = LocalGlassConfig.current

    val containerColor = GlassTopAppBarDefaults.containerColor()
    val scrolledColor = GlassTopAppBarDefaults.scrolledContainerColor()

    val collapsedFraction = scrollBehavior?.collapsedFraction ?: 0f
    val animatedFraction by animateFloatAsState(collapsedFraction, label = "TopBarFraction")
    val animatedColor = lerp(containerColor, scrolledColor, animatedFraction)

    val liquidGlassActive = com.liquidglass.ui.modifier.isLiquidGlassEnabled() &&
        LocalGlassConfig.current.glassLevel >= 1

    val finalModifier = if (liquidGlassActive) {
        // 真液态玻璃：实时采样背景层+内容层（仅当本组件位于内容层之外时安全）
        // 注意：lens 特效要求 CornerBasedShape，矩形需用 RoundedCornerShape(0) 而非 RectangleShape
        modifier.liquidGlass(androidx.compose.foundation.shape.RoundedCornerShape(0.dp))
    } else if (hazeState != null) {
        modifier
            .background(color = animatedColor)
            .glassHazeEffect(state = hazeState)
    } else {
        modifier.background(color = animatedColor)
    }

    val subtitleText = subtitle?.takeIf { it.isNotBlank() }
    val showSubtitle = animatedFraction < 0.5f

    Column(
        modifier = finalModifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(
                    if (config.useFlexibleTopAppBar) {
                        val expanded = FlexibleExpandedHeight.value
                        val collapsed = FlexibleCollapsedHeight.value
                        Dp(
                            androidx.compose.ui.util.lerp(expanded, collapsed, animatedFraction)
                        )
                    } else {
                        FlexibleCollapsedHeight
                    }
                )
        ) {
            // 标题：随折叠进度缩放并上移
            Column(
                modifier = Modifier
                    .align(if (config.useFlexibleTopAppBar) Alignment.BottomStart else Alignment.CenterStart)
                    .padding(start = 16.dp, end = 16.dp)
                    .graphicsLayer {
                        if (config.useFlexibleTopAppBar) {
                            val scale = androidx.compose.ui.util.lerp(1f, 0.75f, animatedFraction)
                            scaleX = scale
                            scaleY = scale
                            translationY = androidx.compose.ui.util.lerp(0f, -4f, animatedFraction)
                        }
                    }
            ) {
                AnimatedContent(
                    targetState = title,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TitleAnimation"
                ) { text ->
                    Text(
                        text = text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                if (subtitleText != null && showSubtitle) {
                    AnimatedContent(
                        targetState = subtitleText,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "SubtitleAnimation"
                    ) { sub ->
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // 导航按钮
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
            ) {
                navigationIcon()
            }
            // actions 行
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            ) {
                GlassTopBarActionsRow { actions() }
            }
        }

        bottomContent?.invoke(this)
    }
}
