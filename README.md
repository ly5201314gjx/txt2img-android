# 文字生图 · txt2img-android（液态玻璃版）

**安卓端多供应商 AI 生图客户端** — Kotlin + Jetpack Compose 原生开发

支持 OpenAI gpt-image 兼容协议 · 多供应商/多模型独立管理 · 参考图多图联动 · Agent 提示词扶正 · 多模态图片反推 · 作品分类管理 · 后台保活 · **全界面液态玻璃 UI**

---

## ✨ 功能特性

### 🎨 生图
- **gpt-image 兼容协议**：`/images/generations`、`/images/edits`（JSON + multipart）、`/chat/completions` 多路径自动降级
- **10 项内置风格**：无风格 / 轻度美颜 / 风景美化 / 主体突出 / 二次元 / 漫画 / 卡通 / 插画 / 艺术 / 水彩
- **参考图多图联动**：最多 3 张参考图（image/image2/image3 内联 base64 + edits 数组 + multipart）
- **生成参数**：比例（1:1 ~ 16:9）、清晰度（标清/高清/超清）、生成数量（1/2/4 张）
- **Agent 提示词扶正**：任意供应商模型优化提示词，预览对比后应用

### 🖼️ 图片反推
- 多模态模型自动**视觉能力测试**（结果缓存）
- 按场景输出结构化提示词：**UI 设计 / 人物 / 风景 / 产品 / 插画**
- 反推结果可复制 / 应用到生成框 / 保存到作品页

### 📁 作品管理
- 自定义分类（新建 / 排序 / 重命名 / 删除）、多选批量操作
- 详情面板：全屏查看 / 下载相册 / 复制提示词 / 二次编辑
- 展示生成耗时与比例

### 💎 液态玻璃 UI（分支特色）
- **真液态玻璃渲染**（Backdrop 实时采样：vibrancy + blur + lens + 高光 + 投影）
- 玻璃顶栏（内容滚动实时模糊）、悬浮导航（可拖拽指示器 + 阻尼动画 + 按压高光 + 速度形变）
- 全部卡片 / 按钮 / 选择器 / 弹窗玻璃化，按压阻尼动效
- 暖杏色系设计（杏橙品牌色 + 琥珀金状态徽章 + 暖奶油背景）
- 弹窗窗口级实时模糊（Android 12+）+ 弹簧入场动画
- 自研组件库见 [liquid-glass 模块](liquid-glass/README.md) 与 [docs](liquid-glass/docs/README.md)

### ⚙️ 多供应商管理
- 折叠卡片管理供应商（名称 / 接口 / API Key），模型子集勾选
- 生图 / Agent / 反推三套模型独立持久化

### 🔋 系统能力
- 前台服务保活（唤醒锁 + 常驻通知）、忽略电池优化引导、完成通知推送

---

## 🛠️ 技术栈

| 项 | 版本 |
|---|---|
| Kotlin | 2.4.0 |
| Jetpack Compose (BOM) | 2026.06.01 |
| Android Gradle Plugin | 9.2.1 |
| Gradle | 9.6.1 |
| minSdk / targetSdk | 26 / 35 |
| 液态玻璃 | [kyant0/backdrop](https://github.com/Kyant0/backdrop) 2.0.0 · [chrisbanes/haze](https://github.com/chrisbanes/haze) 1.7.2 |
| 网络 | OkHttp 4.12 |
| 持久化 | Preferences DataStore |
| 图片加载 | Coil 2.7 |

## 📦 安装

从 [Releases](https://github.com/ly5201314gjx/txt2img-android/releases) 下载最新 APK（Android 8.0+）。

> 包名 `com.txt2img.liquidglass`，与旧版 `com.example.txt2img` 可共存安装。

## 🔨 构建

```bash
# 需要 JDK 17+ 与 Android SDK（local.properties 配置 sdk.dir）
./gradlew :app:assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## 📋 更新日志

见 [CHANGELOG.md](CHANGELOG.md)。

## ⚠️ 免责声明

本应用为个人兴趣项目，不用于任何商业用途，不以盈利为目的，仅作为文字生图工具使用。
