package com.liquidglass.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/**
 * 液态玻璃宿主（架构对齐原项目 AppScaffold）：
 *
 * ```
 * Box
 * ├── 背景层 Box（layerBackdrop 背景层）→ 渐变/壁纸
 * ├── 内容层 Box（layerBackdrop 内容层）→ 页面内容（不含任何 drawBackdrop 组件）
 * ├── 顶栏（topBar，不在任何层内）→ 玻璃顶栏（drawBackdrop 的宿主）
 * └── 浮层 Box（顶层，不在任何层内）→ 底部导航等玻璃组件（drawBackdrop 的宿主）
 * ```
 *
 * 铁律：**所有 drawBackdrop 的玻璃组件必须放在 [topBar]/[overlay] 里**，
 * 不能放进 [content]，否则玻璃组件会采样自己所在的层导致递归崩溃。
 *
 * 提供：
 * - [LocalGlassBackdrop]：合并后的 Backdrop（背景层 + 内容层）
 * - [LocalGlassHazeState]：HazeState（enableBlur 时才非 null）
 * - [LocalGlassConfig]：全局配置
 */
@Composable
fun LiquidGlassHost(
    modifier: Modifier = Modifier,
    config: GlassConfig = GlassConfig.Default,
    backgroundColor: Color = Color.Transparent,
    background: @Composable BoxScope.() -> Unit = {},
    topBar: @Composable () -> Unit = {},
    content: @Composable BoxScope.(PaddingValues) -> Unit,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val hazeState = remember { HazeState() }
    val liquidEnabled = config.topBarButtonStyle == TopBarButtonStyle.LiquidGlass

    // 背景层：绘制底色 + 背景内容，作为玻璃的底层来源
    val topBarBackgroundBackdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
    // 内容层：页面内容，作为玻璃的上层来源
    val topBarContentBackdrop = rememberLayerBackdrop { drawContent() }
    val topBarBackdrop = rememberCombinedBackdrop(
        topBarBackgroundBackdrop,
        topBarContentBackdrop
    )

    CompositionLocalProvider(
        LocalGlassConfig provides config,
        LocalGlassHazeState provides if (config.enableBlur) hazeState else null,
        LocalGlassBackdrop provides if (liquidEnabled) topBarBackdrop else null,
        LocalGlassBackgroundBackdrop provides if (liquidEnabled) topBarBackgroundBackdrop else null,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            // 背景层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (liquidEnabled) {
                            Modifier.layerBackdrop(topBarBackgroundBackdrop)
                        } else {
                            Modifier
                        }
                    )
            ) {
                background()
            }
            // 内容层（页面内容，不含玻璃组件）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (liquidEnabled) {
                            Modifier.layerBackdrop(topBarContentBackdrop)
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (config.enableBlur) Modifier.hazeSource(hazeState)
                        else Modifier
                    )
            ) {
                content(PaddingValues(0.dp))
            }
            // 顶栏：玻璃组件（drawBackdrop）的宿主，位于内容层之外，绘制在内容之上
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    topBar()
                }
            }
            // 浮层：底部导航等玻璃组件（drawBackdrop）的宿主，位于所有层之外
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                overlay()
            }
        }
    }
}
