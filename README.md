# 文字生图 (txt2img-android)

安卓端 AI 生图客户端，Kotlin + Jetpack Compose 原生开发。

## 功能

- **多供应商模型管理**：可添加多个第三方供应商（接口地址 + API Key），三角折叠卡片管理，自动拉取模型列表
- **gpt-image 兼容生图**：OpenAI 兼容协议（`/images/generations`、`/images/edits`、chat 兜底），多格式响应解析（`data[]` / `images[]` / chat image_url / data URI / b64）
- **生成参数**：比例（1:1 ~ 16:9）、清晰度（标清/高清/超清）、生成数量（1/2/4 张）、风格（无风格 + 5 种预设）、参考图上传（图生图）
- **作品库**：生成图片自动持久化，支持自定义分类、长按归类/删除、详情面板（下载到相册 / 复制提示词 / 保存参考图）
- **后台保活**：生成期间前台服务 + 唤醒锁，切后台/锁屏不中断；支持请求忽略电池优化
- **通知推送**：生图完成自动发送通知栏推送，点击直达

## 技术栈

| 项 | 版本 |
|---|---|
| Kotlin | 2.0.21 |
| Jetpack Compose (BOM) | 2024.12.01 |
| AGP | 8.7.2 |
| Gradle | 8.13 |
| minSdk / targetSdk | 26 / 35 |
| 网络 | OkHttp 4.12 |
| 持久化 | Preferences DataStore |
| 图片加载 | Coil 2.7 |

## 构建

```bash
# 需要 JDK 17+ 与 Android SDK（local.properties 配置 sdk.dir）
gradle assembleRelease
# 产物：app/build/outputs/apk/release/app-release.apk
```

## 更新日志

见 GitHub Releases。

## 免责声明

本应用为个人兴趣项目，不用于商业用途，仅作为文字生图工具使用。
