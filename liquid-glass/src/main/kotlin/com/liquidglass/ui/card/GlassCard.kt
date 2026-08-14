package com.liquidglass.ui.card

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liquidglass.ui.LocalGlassConfig

/**
 * 玻璃卡片：容器色按 [LocalGlassConfig.containerOpacity] 半透明化，
 * 适合放在玻璃背景/模糊内容之上。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    containerColor: Color? = null,
    contentColor: Color? = null,
    elevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedContainerColor = (containerColor ?: MaterialTheme.colorScheme.surfaceContainer)
        .let {
            it.copy(alpha = it.alpha * LocalGlassConfig.current.containerOpacity.coerceIn(0, 100) / 100f)
        }
    val resolvedShape = RoundedCornerShape(cornerRadius)
    val clickableModifier = if (onClick != null || onLongClick != null) {
        modifier
            .clip(resolvedShape)
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick
            )
    } else {
        modifier
    }
    Surface(
        modifier = clickableModifier,
        shape = resolvedShape,
        color = if (containerColor == Color.Transparent) Color.Transparent else resolvedContainerColor,
        contentColor = contentColor ?: MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = elevation,
        border = border
    ) {
        Column(modifier = Modifier, content = content)
    }
}

/**
 * 普通卡片（不透明，无玻璃效果）。
 */
@Composable
fun NormalCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    cornerRadius: Dp = 16.dp,
    containerColor: Color? = null,
    contentColor: Color? = null,
    elevation: Dp = 0.dp,
    border: BorderStroke? = null,
    shape: Shape = RoundedCornerShape(cornerRadius),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor ?: MaterialTheme.colorScheme.surfaceContainer,
        contentColor = contentColor ?: MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = elevation,
        border = border
    ) {
        Column(modifier = Modifier, content = content)
    }
}
