package com.liquidglass.ui.topbar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 顶栏滚动行为抽象。
 */
sealed interface GlassTopAppBarScrollBehavior {
    /** 滚动折叠进度 0f（展开）~ 1f（完全折叠） */
    val collapsedFraction: Float

    /** 挂到内容的 nestedScroll 上以感知滚动 */
    val nestedScrollConnection: NestedScrollConnection
}

/** 自实现滚动行为（不依赖 material3 实验性 API） */
class M3GlassScrollBehavior(
    private val state: GlassScrollState,
) : GlassTopAppBarScrollBehavior {
    override val collapsedFraction: Float get() = state.collapsedFraction
    override val nestedScrollConnection: NestedScrollConnection
        get() = state.nestedScrollConnection
}

/**
 * 创建顶栏滚动行为。
 *
 * 用法：
 * ```kotlin
 * val scrollBehavior = rememberGlassTopAppBarScrollBehavior()
 * Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
 *     GlassMediumFlexibleTopAppBar(scrollBehavior = scrollBehavior)
 * }
 * ```
 */
@Composable
fun rememberGlassTopAppBarScrollBehavior(): GlassTopAppBarScrollBehavior {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    return remember(density, scope) {
        M3GlassScrollBehavior(GlassScrollState(density, scope))
    }
}

/**
 * 顶栏滚动状态：跟随内容滚动在「展开 ↔ 折叠」之间切换。
 *
 * - 上滑（内容向下滚）：顶栏折叠
 * - 下滑（内容向上滚）：顶栏展开
 * - fling：弹簧动画吸附到两端
 *
 * 内部用 [Animatable] 驱动 [collapsedFraction]（0f 展开 ~ 1f 折叠）。
 */
class GlassScrollState(
    private val density: Density,
    private val animationScope: CoroutineScope,
) {
    /** 折叠总位移（px） */
    private val maxOffset: Int
        get() = with(density) {
            (GlassFlexibleExpandedHeight - GlassFlexibleCollapsedHeight).roundToPx()
        }.coerceAtLeast(0)

    private val offsetAnimatable = Animatable(0f)

    /** 当前折叠偏移（px），0 = 完全折叠 */
    private val currentOffset: Float get() = offsetAnimatable.value

    /** 折叠进度 0f（展开）~ 1f（完全折叠） */
    val collapsedFraction: Float
        get() = if (maxOffset == 0) 0f else (currentOffset / maxOffset).coerceIn(0f, 1f)

    private fun consumeScrollDelta(delta: Float): Offset {
        val old = currentOffset
        val new = (old + delta).coerceIn(0f, maxOffset.toFloat())
        animationScope.launch { offsetAnimatable.snapTo(new) }
        return Offset(0f, new - old)
    }

    private fun flingTo(target: Float) {
        animationScope.launch {
            offsetAnimatable.animateTo(
                targetValue = target,
                animationSpec = spring(1f, 1000f, 0.01f)
            )
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y
            // 手指上滑（内容向下滚）：优先折叠顶栏
            return if (delta < 0 && currentOffset < maxOffset) {
                consumeScrollDelta(delta)
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val delta = available.y
            // 手指下滑（内容向上滚）：展开顶栏
            return if (delta > 0 && currentOffset > 0f) {
                consumeScrollDelta(delta)
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val velocity = available.y
            if (velocity == 0f) return Velocity.Zero
            // 上滑 fling → 完全折叠；下滑 fling → 完全展开
            flingTo(if (velocity < 0f) maxOffset.toFloat() else 0f)
            return Velocity(0f, velocity)
        }
    }
}
