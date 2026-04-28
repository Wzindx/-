# 通用图像工坊

一个 Android 原生通用 OpenAI Images API 兼容客户端，参考 `CookSleep/gpt_image_playground` 的图像生成/编辑体验，并参考海鸥云 Images API 文档实现 `/v1/images/generations` 与 `/v1/images/edits` 调用。

## 功能

- 支持任意 OpenAI Images API 兼容站点，不绑定具体服务商
- 可配置 Base URL，例如：
  - `https://api.openai.com/v1`
  - `https://example.com/v1`
  - `https://www.hohy6.com/v1`
- 可配置 API Key 与任意模型名
- 文生图：`POST /v1/images/generations`
- 图生图/图片编辑：`multipart POST /v1/images/edits`
- 兼容返回 `data[0].url` 与 `data[0].b64_json`
- 支持长超时，适合高质量生图
- 支持尺寸、质量、数量、输出格式、background 参数
- 支持选择本地参考图
- 支持图片预览与保存到系统相册
- 支持简单本地历史记录

## 默认配置

默认 Base URL 为：

```text
https://api.openai.com/v1
```

你可以在 App 内修改为任意兼容站点。

## GitHub Actions 构建

项目已包含：

```text
.github/workflows/android.yml
```

推送到 GitHub 后，进入 Actions 手动运行或 push 到 main/master，即可生成 Debug APK，构建产物会上传为 artifact。

## 包名

```text
com.operit.hohyaiimage
```

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- HttpURLConnection，无额外网络库依赖
