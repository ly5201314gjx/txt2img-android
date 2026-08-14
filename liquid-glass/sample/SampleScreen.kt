/*
 * 使用示例（复制到你的项目即可运行）。
 *
 * 依赖：项目根 build.gradle.kts 需包含
 *   implementation("io.github.kyant0:backdrop:2.0.0")
 *   implementation("dev.chrisbanes.haze:haze:1.7.2")
 *   implementation("io.github.kyant0:capsule:2.1.3")
 *   implementation(platform("androidx.compose:compose-bom:<你的 BOM>"))
 *
 * 注意：液态玻璃渲染需要 Android 13+（TIRAMISU）。
 */

package com.example.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.liquidglass.ui.GlassConfig
import com.liquidglass.ui.LiquidGlassHost
import com.liquidglass.ui.TopBarButtonStyle
import com.liquidglass.ui.bottombar.FloatingBottomBar
import com.liquidglass.ui.bottombar.FloatingBottomBarItem
import com.liquidglass.ui.button.GlassTopBarActionButton
import com.liquidglass.ui.button.GlassTopBarNavigationButton
import com.liquidglass.ui.card.GlassCard
import com.liquidglass.ui.topbar.GlassMediumFlexibleTopAppBar
import com.liquidglass.ui.topbar.rememberGlassTopAppBarScrollBehavior

@Preview
@Composable
fun LiquidGlassSample() {
    var selectedTab by remember { mutableIntStateOf(0) }
    var scroll by remember { mutableFloatStateOf(0f) }

    MaterialTheme {
        LiquidGlassHost(
            config = GlassConfig(
                enableBlur = true,
                enableProgressiveBlur = true,
                topBarButtonStyle = TopBarButtonStyle.LiquidGlass,
                mergeTopBarActions = true,
                useFlexibleTopAppBar = true,
                bottomBarBlurAlpha = 80,
            ),
            backgroundColor = MaterialTheme.colorScheme.surface,
        ) {
            // 背景层：任何内容都会透过玻璃显示出来
            AsyncImage(
                model = "https://example.com/your-wallpaper.jpg",
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } content = { _ ->
            val scrollBehavior = rememberGlassTopAppBarScrollBehavior()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = Color.Transparent,
                topBar = {
                    GlassMediumFlexibleTopAppBar(
                        title = "液态玻璃示例",
                        subtitle = "Material 3 · Backdrop 实时渲染",
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            GlassTopBarNavigationButton(
                                onClick = {},
                                contentDescription = "返回",
                            )
                        },
                        actions = {
                            GlassTopBarActionButton(
                                onClick = {},
                                imageVector = Icons.Filled.Search,
                                contentDescription = "搜索",
                            )
                            GlassTopBarActionButton(
                                onClick = {},
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "菜单",
                            )
                        },
                    )
                },
                bottomBar = {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        FloatingBottomBar(
                            selectedIndex = { selectedTab },
                            onSelected = { selectedTab = it },
                            tabsCount = 3,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        ) {
                            FloatingBottomBarItem(
                                onClick = { selectedTab = 0 },
                            ) {
                                Icon(
                                    Icons.Filled.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text("首页", style = MaterialTheme.typography.labelMedium)
                            }
                            FloatingBottomBarItem(
                                onClick = { selectedTab = 1 },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text("发现", style = MaterialTheme.typography.labelMedium)
                            }
                            FloatingBottomBarItem(
                                onClick = { selectedTab = 2 },
                            ) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text("设置", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                },
                content = { padding ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        itemsIndexed((1..20).toList()) { index, _ ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                            ) {
                                Text(
                                    text = "玻璃卡片 #$index",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}
