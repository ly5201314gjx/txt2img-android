package com.liquidglass.ui.button

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.liquidglass.ui.GlassDefaults
import com.liquidglass.ui.LocalGlassConfig
import com.liquidglass.ui.TopBarButtonStyle
import com.liquidglass.ui.modifier.isLiquidGlassEnabled
import com.liquidglass.ui.modifier.liquidGlass
import com.liquidglass.ui.topbar.GlassTopAppBarDefaults

/** 合并模式下按钮间是否画竖向分隔线（首个按钮不画） */
internal val LocalTopBarMergeState = staticCompositionLocalOf { false }

/** 胶囊合并模式下的按钮尺寸 */
internal val MergeButtonSize = 36.dp

@Composable
private fun currentTopBarButtonStyle(): TopBarButtonStyle =
    LocalGlassConfig.current.topBarButtonStyle

@Composable
internal fun topBarActionSpacing(): androidx.compose.ui.unit.Dp {
    return if (currentTopBarButtonStyle() == TopBarButtonStyle.Plain) 4.dp else 8.dp
}

/** 合并模式下按钮左侧的竖向分隔线（首个按钮不画）。 */
@Composable
private fun Modifier.mergedDivider(): Modifier {
    var showDivider by remember { mutableStateOf(false) }
    val dividerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    return onPlaced { coordinates ->
        showDivider = coordinates.positionInParent().x > 0f
    }.then(
        if (showDivider) {
            Modifier.drawBehind {
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, size.height * 0.3f),
                    end = Offset(0f, size.height * 0.7f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        } else {
            Modifier
        }
    )
}

/**
 * 顶栏 actions 的统一 Row。
 *
 * 开启「合并顶栏按钮」且样式为 Tonal/Outlined/半透明/液态玻璃时，把多个按钮的
 * 容器融合成一个胶囊，按钮间用竖向分隔线隔开。
 * 单个按钮时胶囊自然退化为普通按钮。Plain 无容器，始终走普通间距 Row。
 */
@Composable
fun GlassTopBarActionsRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val style = currentTopBarButtonStyle()
    val mergeEnabled = LocalGlassConfig.current.mergeTopBarActions
    val liquidGlassEnabled = style == TopBarButtonStyle.LiquidGlass &&
            isLiquidGlassEnabled()
    if (!mergeEnabled || style == TopBarButtonStyle.Plain) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(topBarActionSpacing()),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        return
    }

    val capsuleShape = RoundedCornerShape(50)
    val capsuleBg = when (style) {
        TopBarButtonStyle.Tonal -> MaterialTheme.colorScheme.surfaceContainerLow
        TopBarButtonStyle.SemiTransparent, TopBarButtonStyle.LiquidGlass ->
            GlassTopAppBarDefaults.controlContainerColor()
        else -> Color.Transparent // Outlined
    }
    Box(
        modifier = modifier
            .height(MergeButtonSize)
            .then(if (!liquidGlassEnabled) Modifier.clip(capsuleShape) else Modifier)
            .then(
                if (liquidGlassEnabled) {
                    Modifier.liquidGlass(capsuleShape)
                } else {
                    Modifier.background(capsuleBg, capsuleShape)
                }
            )
            .then(
                if (style == TopBarButtonStyle.Outlined) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, capsuleShape)
                } else {
                    Modifier
                }
            )
    ) {
        CompositionLocalProvider(LocalTopBarMergeState provides true) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@Composable
private fun TopBarButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageVector: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    contentDescription: String? = null,
    style: TopBarButtonStyle = currentTopBarButtonStyle()
) {
    val isMerged = LocalTopBarMergeState.current
    val liquidGlassEnabled = style == TopBarButtonStyle.LiquidGlass &&
            isLiquidGlassEnabled()
    val buttonSize = if (style == TopBarButtonStyle.Plain) 40.dp else MergeButtonSize
    val iconSize = if (style == TopBarButtonStyle.Plain) 24.dp else 20.dp

    if (isMerged) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(buttonSize)
                .mergedDivider()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = if (liquidGlassEnabled) null else ripple(bounded = true),
                    role = Role.Button,
                    onClick = onClick
                )
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(iconSize)
            )
        }
    } else {
        val containerColor = when {
            liquidGlassEnabled -> Color.Transparent
            style == TopBarButtonStyle.SemiTransparent ||
                    style == TopBarButtonStyle.LiquidGlass ->
                GlassTopAppBarDefaults.controlContainerColor()

            style == TopBarButtonStyle.Tonal -> MaterialTheme.colorScheme.surfaceContainerLow
            style == TopBarButtonStyle.Outlined -> Color.Transparent
            else -> null
        }
        val shape = RoundedCornerShape(50)
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(buttonSize)
                .then(
                    if (liquidGlassEnabled) {
                        Modifier.liquidGlass(shape)
                    } else {
                        Modifier.clip(shape).background(containerColor ?: Color.Transparent)
                    }
                )
                .then(
                    if (style == TopBarButtonStyle.Outlined) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    } else {
                        Modifier
                    }
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = if (liquidGlassEnabled) null else ripple(bounded = true),
                    role = Role.Button,
                    onClick = onClick
                )
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * 顶栏返回按钮（液态玻璃样式）。
 */
@Composable
fun GlassTopBarNavigationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageVector: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    contentDescription: String? = null,
) {
    TopBarButton(
        onClick = onClick,
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.padding(horizontal = 12.dp)
    )
}

/**
 * 顶栏动作按钮（液态玻璃样式）。
 */
@Composable
fun GlassTopBarActionButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    TopBarButton(
        onClick = onClick,
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier
    )
}

/**
 * 带文字的动作按钮（液态玻璃样式）。
 */
@Composable
fun GlassTopBarTextActionButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
) {
    val style = currentTopBarButtonStyle()
    val liquidGlassEnabled = style == TopBarButtonStyle.LiquidGlass &&
            isLiquidGlassEnabled()
    val containerColor = when {
        liquidGlassEnabled -> Color.Transparent
        style == TopBarButtonStyle.SemiTransparent ||
                style == TopBarButtonStyle.LiquidGlass ->
            GlassTopAppBarDefaults.controlContainerColor()

        style == TopBarButtonStyle.Tonal -> MaterialTheme.colorScheme.surfaceContainerLow
        style == TopBarButtonStyle.Outlined -> Color.Transparent
        else -> null
    }
    val shape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(MergeButtonSize)
            .then(
                if (liquidGlassEnabled) {
                    Modifier.liquidGlass(shape)
                } else {
                    Modifier.clip(shape).background(containerColor ?: Color.Transparent)
                }
            )
            .then(
                if (style == TopBarButtonStyle.Outlined) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (liquidGlassEnabled) null else ripple(bounded = true),
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
