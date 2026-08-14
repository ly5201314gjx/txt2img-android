# LiquidGlass UI

从 [Legado with MD3](https://github.com/HapeLee/legado-with-MD3) 中独立摘取的
**液态玻璃（Liquid Glass）UI 组件库**：Material 3 风格的实时模糊玻璃容器 + 按压/拖拽交互动画，
开箱即用、零业务耦合。

![Android](https://img.shields.io/badge/Android-13%2B-%2334a853) ![Compose](https://img.shields.io/badge/Jetpack%20Compose-required-4285F4)

## 特性

- **液态玻璃容器**：`Modifier.liquidGlass(shape)` —— 对底层画面实时采样，`vibrancy`（活力增强）+ `blur`（12dp 模糊）+ `lens`（镜头扭曲）+ 高光描边 + 柔和投影
- **按压交互动画**：`InteractiveHighlight` —— AGSL RuntimeShader 径向高光跟随手指、玻璃随按压放大/位移（tanh 阻尼）、松手弹簧回弹
- **阻尼拖拽动画**：`DampedDragAnimation` —— 按住放大、松手吸附回弹、全程速度追踪（驱动底部栏 lens/形变）
- **玻璃顶栏**：固定式 `GlassTopAppBar` / 可折叠 `GlassMediumFlexibleTopAppBar`，滚动插值、渐进式模糊、多按钮自动合并为胶囊
- **玻璃按钮**：5 种样式（Plain / Tonal / Outlined / SemiTransparent / LiquidGlass），液态玻璃样式禁用 ripple（改用高光反馈）
- **液态玻璃底部导航**：`FloatingBottomBar` —— 底座 + 指示器双层玻璃，指示器可拖拽切换 Tab
- **玻璃卡片**：`GlassCard` 半透明卡片
- **Haze 兜底**：Android 13 以下自动退化为 `haze` 传统模糊；`enableBlur=false` 时退化为普通半透明容器

## 目录结构

```
src/main/kotlin/com/liquidglass/ui/
├── LiquidGlassHost.kt        # 宿主：注册 Backdrop 层 + HazeState，提供 CompositionLocal
├── GlassConfig.kt            # 全局配置 + LocalGlassConfig / LocalGlassBackdrop / LocalGlassHazeState
├── GlassDefaults.kt          # 玻璃颜色工具
├── modifier/LiquidGlass.kt   # Modifier.liquidGlass(shape) 核心渲染 + isLiquidGlassEnabled()
├── animation/
│   ├── InteractiveHighlight.kt   # 按压高光动画（AGSL 径向渐变）
│   ├── DampedDragAnimation.kt    # 阻尼拖拽动画
│   └── DragGestureInspector.kt   # 观察式拖拽手势（不消费事件）
├── haze/GlassHaze.kt         # glassHazeSource / glassHazeEffect（Haze 兜底）
├── topbar/                   # GlassTopAppBar / GlassMediumFlexibleTopAppBar / GlassScrollState
├── button/                   # GlassTopBarActionButton / GlassTopBarNavigationButton / 合并胶囊
├── card/GlassCard.kt         # 玻璃卡片
└── bottombar/FloatingBottomBar.kt  # 液态玻璃底部导航
```

## 快速开始

### 1. 引入模块

把 `liquid-glass` 目录复制进你的项目：

```kotlin
// settings.gradle.kts
include(":liquid-glass")
```

```kotlin
// 项目根 build.gradle.kts 中声明依赖（或写进你自己的 version catalog）
dependencies {
    implementation("io.github.kyant0:backdrop:2.0.0")   // 液态玻璃渲染
    implementation("dev.chrisbanes.haze:haze:1.7.2")    // Haze 兜底模糊
    implementation("io.github.kyant0:capsule:2.1.3")    // 连续胶囊形状
}
```

要求：Compose BOM 2025.02.00+、`kotlinOptions.jvmTarget = 17`、`compileSdk >= 35`。

### 2. 包一层宿主

`LiquidGlassHost` 负责把「背景层 + 内容层」注册为 Backdrop 采样源：

```kotlin
LiquidGlassHost(
    config = GlassConfig(enableBlur = true, mergeTopBarActions = true),
    backgroundColor = MaterialTheme.colorScheme.surface,
) {
    // 背景层：任何内容都会透过玻璃显示
    AsyncImage(model = wallpaper, contentDescription = null, modifier = Modifier.fillMaxSize())
} content = { _ ->
    Scaffold(
        topBar = { GlassMediumFlexibleTopAppBar(title = "标题") },
        bottomBar = { /* FloatingBottomBar(...) */ },
    ) { padding -> ... }
}
```

### 3. 直接用组件

| 组件 | 说明 |
|---|---|
| `GlassMediumFlexibleTopAppBar(title, scrollBehavior = rememberGlassTopAppBarScrollBehavior())` | 可折叠玻璃顶栏（滚动折叠自实现，无需实验性 API） |
| `GlassTopBarNavigationButton(onClick, contentDescription)` | 返回按钮 |
| `GlassTopBarActionButton(onClick, imageVector, contentDescription)` | 动作按钮 |
| `GlassTopBarActionsRow { ... }` | 按钮行（自动合并胶囊） |
| `GlassCard { ... }` | 玻璃卡片 |
| `FloatingBottomBar(selectedIndex, onSelected, tabsCount) { FloatingBottomBarItem(...) }` | 底部导航 |

折叠顶栏用法：

```kotlin
val scrollBehavior = rememberGlassTopAppBarScrollBehavior()
Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
    GlassMediumFlexibleTopAppBar(title = "标题", scrollBehavior = scrollBehavior)
}
```

### 4. 高级：对任意组件施加液态玻璃

```kotlin
Modifier.liquidGlass(RoundedCornerShape(24.dp))
```

配合 `isLiquidGlassEnabled()` 判断是否生效（需在 Host 内部 + Android 13+）。

完整可运行示例见 [`sample/SampleScreen.kt`](sample/SampleScreen.kt)。

## 配置项（GlassConfig）

| 字段 | 默认值 | 说明 |
|---|---|---|
| `enableBlur` | `true` | Haze 兜底模糊开关 |
| `enableProgressiveBlur` | `false` | 渐进式模糊（上 100% → 下 0%） |
| `topBarBlurRadius` | `24` | 顶栏模糊半径 |
| `topBarBlurAlpha` | `73` | 顶栏模糊透明度 |
| `topBarOpacity` | `100` | 顶栏容器不透明度 |
| `topBarButtonStyle` | `LiquidGlass` | 按钮样式枚举 |
| `mergeTopBarActions` | `false` | 按钮合并为胶囊 |
| `useFlexibleTopAppBar` | `true` | 使用可折叠 MediumFlexibleTopAppBar |
| `bottomBarBlurRadius` | `24f` | 底部栏模糊半径 |
| `bottomBarBlurAlpha` | `80` | 底部栏不透明度 |
| `bottomBarLensRadius` | `24.dp` | 底部栏镜头半径 |
| `containerOpacity` | `100` | 玻璃卡片不透明度 |
| `accentColor` | `Unspecified` | 底部栏图标强调色 |

## 版本要求

- **Android 13+（TIRAMISU）**：液态玻璃渲染（backdrop / AGSL 高光）完整可用
- **Android 8+（API 26）**：自动退化为 Haze 模糊 + 半透明容器，交互动画仍可用
- JDK 17、Kotlin 2.x、AGP 8.x

## 依赖

- [kyant0/backdrop](https://github.com/Kyant0/backdrop) 2.0.0 —— 液态玻璃实时渲染
- [chrisbanes/haze](https://github.com/chrisbanes/haze) 1.7.2 —— 传统背景模糊
- [kyant0/capsule](https://github.com/Kyant0/capsule) 2.1.3 —— 连续胶囊形状
- androidx.compose (BOM 2025.02.00+)、kotlinx-coroutines

## License

组件代码衍生自 [Legado with MD3](https://github.com/HapeLee/legado-with-MD3)（GPL-3.0），
FloatingBottomBar 部分衍生自 [weishu/KernelSU](https://github.com/tiann/KernelSU)（GPL-3.0）。
请遵守 GPL-3.0 许可证。
