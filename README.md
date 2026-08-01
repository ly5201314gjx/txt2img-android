<div align="center">

# 文字生图 · txt2img-android

**安卓端多供应商 AI 生图客户端** — Kotlin + Jetpack Compose 原生开发

支持 OpenAI gpt-image 兼容协议 · 多供应商/多模型独立管理 · 参考图多图联动 · Agent 提示词扶正 · 多模态图片反推 · 作品分类管理 · 后台保活

</div>

---

## ✨ 功能特性

### 🎨 生图
- **gpt-image 兼容协议**：`/images/generations`、`/images/edits`（JSON + multipart）、`/chat/completions` 多路径自动降级
- **10 项内置风格**：无风格 / 轻度美颜 / 风景美化 / 主体突出 / 二次元 / 漫画 / 卡通 / 插画 / 艺术 / 水彩，选择风格后可不填提示词直接生图
- **参考图多图联动**：最多 3 张参考图（image/image2/image3 内联 base64 + edits 数组 + multipart）
- **生成参数**：比例（1:1 ~ 16:9）、清晰度（标清/高清/超清）、生成数量（1/2/4 张）
- **Agent 提示词扶正**：选用任意供应商模型优化提示词，预览对比后应用

### 🖼️ 图片反推
- 多模态模型选择后自动**视觉能力测试**（结果缓存）
- 按场景输出结构化细腻提示词：**UI 设计 / 人物 / 风景 / 产品 / 插画**
- 反推结果可复制 / 应用到生成框 / 保存到作品页（带「反推」角标）

### 📁 作品管理
- 生成图片自动持久化保存，支持**自定义分类**（新建 / 排序 / 重命名 / 删除）
- **多选模式**：批量移动分类 / 批量删除
- 详情面板：全屏查看 / 下载到相册 / 复制提示词 / 保存参考图 / 二次编辑
- 展示生成**耗时与比例**

### ⚙️ 多供应商管理
- 三角折叠卡片管理多个供应商（名称 / 接口地址 / API Key）
- 每个供应商可获取并**勾选展示**模型子集
- **生图 / Agent / 反推**三套模型完全独立持久化，互不干扰

### 🔋 系统能力
- 生图 / 反推期间**前台服务保活**（唤醒锁 + 常驻通知），切后台不中断
- **忽略电池优化**引导（首次启动弹窗 + 「我的」页入口）
- 生图完成 / 反推完成**通知栏推送**

---

## 🛠️ 技术栈

| 项 | 版本 |
|---|---|
| Kotlin | 2.0.21 |
| Jetpack Compose (BOM) | 2024.12.01 |
| Android Gradle Plugin | 8.7.2 |
| Gradle | 8.13 |
| minSdk / targetSdk | 26 / 35 |
| 网络 | OkHttp 4.12 |
| 持久化 | Preferences DataStore |
| 图片加载 | Coil 2.7 |

---

## 📦 安装

从 [Releases](https://github.com/ly5201314gjx/txt2img-android/releases) 下载最新 APK 直接安装（Android 8.0+）。

## 🔨 构建

```bash
# 需要 JDK 17+ 与 Android SDK（local.properties 配置 sdk.dir）
gradle assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## 📋 更新日志

见 [GitHub Releases](https://github.com/ly5201314gjx/txt2img-android/releases)。

## ⚠️ 免责声明

本应用为个人兴趣项目，不用于任何商业用途，不以盈利为目的，仅作为文字生图工具使用。
