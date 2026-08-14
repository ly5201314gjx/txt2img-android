# 07 · 实战案例：txt2img 生图 App 接入记录

> 从零把液态玻璃接入一个真实 Compose App 的全过程，
> 包括踩过的所有坑与最终方案。

## 项目背景

- 原项目：[txt2img-android](https://github.com/ly5201314gjx/txt2img-android)（Kotlin + Compose 生图客户端）
- 目标：底部导航、顶栏、卡片、选择器等全部液态玻璃化
- 设备：OPPO PKB110（Android 16 / API 36）

## 阶段一：工具链升级（必做）

backdrop 2.0.0 要求 Kotlin 2.3+，原项目是 Kotlin 2.0.21 / AGP 8.7.2：

```
Kotlin 2.0.21 → 2.4.0        （删除 org.jetbrains.kotlin.android 插件，AGP 9 内置）
AGP 8.7.2 → 9.2.1
Gradle 8.13 → 9.6.1
Compose BOM 2024.12 → 2026.06.01
material3 1.3.x → 1.5.0-alpha23
compileSdk 35 → 37
```

同时修改包名（`com.txt2img.liquidglass`）+ 版本号，避免与已装版本冲突。

## 阶段二：接入方式演进（踩坑史）

### ❌ v1-v3：全部组件塞进内容层

宿主只有一个 content 槽，整个 app（含画玻璃的导航）都在内容层内 →
**自采样递归 → 原生闪退**（无 Java 日志，极难排查）。

### ❌ v4-v6：加安全模式 + 看门狗，仍崩

安全模式（默认关玻璃）能开了，但开玻璃必崩。
`combined(背景层, 背景层)` 重复合并同一层，也是错误姿势。

### ✅ v7：对齐原项目架构

研究 [legado-with-MD3](https://github.com/HapeLee/legado-with-MD3)（液态玻璃来源项目）后复刻其结构：

- 玻璃组件移到**内容层之外**（新增 topBar / overlay 槽）
- 内容层内卡片改回视觉玻璃（glassCard）
- 状态上提（currentTab 提升到 MainActivity），导航条放 overlay

### ✅ v8-v9：自动分级测试

设备原生崩溃无法捕获 → 设计 0/1/2 三级玻璃，自动重启分级：
崩了降级、活了升级、自动稳定在可用最高级。OPPO 设备稳定在 **L2 满特效**。

### ✅ v10：顶栏真玻璃

顶栏移出内容层，内容滚动时实时模糊 —— 对齐原项目观感。

### ❌ v11：RectangleShape 崩溃

顶栏玻璃传了 `RectangleShape` → lens 特效抛 `UnsupportedOperationException`。
修复：`RoundedCornerShape(0.dp)` + 库内形状检测加固。

### ✅ v12-v14：完整版

- 原版 `FloatingBottomBar`（可拖拽指示器、速度形变、按压高光）
- 导航条 `fillMaxWidth` 修复（此前漏了 → 缩成圆）
- 尺寸微调（64dp → 56dp）
- 首页全部控件玻璃化（glassField / glassChip）

## 最终结构

```
MainActivity
├── LiquidGlassHost
│   ├── background = ScreenBackdrop（渐变+光斑）
│   ├── content = GenerateScreen（三个 Tab，内容层内全部视觉玻璃）
│   ├── topBar = GlassMediumFlexibleTopAppBar（真玻璃，按 Tab 切换标题）
│   └── overlay = FloatingBottomBar（真玻璃全动效）+ 玻璃开关
└── 自动分级状态机（glass_v8_state/armed/boot/safe 文件标志）
```

## 关键代码模式

```kotlin
// 内容层内的卡片/控件（视觉玻璃）
Modifier.glassCard(RoundedCornerShape(12.dp))   // 大卡片
Modifier.glassField(RoundedCornerShape(8.dp))   // 输入框/选择器
Modifier.glassChip(RoundedCornerShape(10.dp))   // chips/药丸

// 内容层外（真玻璃）
// topBar 槽：GlassMediumFlexibleTopAppBar（自动检测 backdrop）
// overlay 槽：FloatingBottomBar + FloatingBottomBarItem

// 远程诊断
Thread.setDefaultUncaughtExceptionHandler { _, e ->
    uploadToServer("crash.log = " + Log.getStackTraceString(e))
}
```

## 参数（最终调优值）

- 底部导航：base 56dp / 内层 48dp / pressedScale 66÷48
- 顶栏 blur 12dp（liquidGlass 默认）、表面 surface 50%
- 首页视觉玻璃：glassField 28% 白、glassChip 55% 白
