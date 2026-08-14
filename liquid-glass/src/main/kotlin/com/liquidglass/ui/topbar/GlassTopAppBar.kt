package com.liquidglass.ui.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.liquidglass.ui.LocalGlassHazeState
import com.liquidglass.ui.button.GlassTopBarActionsRow
import com.liquidglass.ui.haze.glassHazeEffect

/**
 * 固定（pinned）玻璃顶栏。
 * 容器色随玻璃配置动态变化，滚动时不变。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GlassTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val hazeState = LocalGlassHazeState.current
    val containerColor = GlassTopAppBarDefaults.containerColor()

    val finalModifier = if (hazeState != null) {
        modifier
            .background(color = containerColor)
            .glassHazeEffect(state = hazeState)
    } else {
        modifier.background(color = containerColor)
    }

    Column(modifier = finalModifier) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            navigationIcon = navigationIcon,
            actions = {
                Box(modifier = Modifier.padding(end = 12.dp)) {
                    GlassTopBarActionsRow { actions() }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            )
        )
    }
}
