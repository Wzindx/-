
# 通用图像工坊

通用图像工坊是一个 Android 原生 OpenAI 兼容图像生成客户端，支持文生图、图生图/图片编辑、多接口模式切换、参考图选择、图片保存和本地历史记录。

项目目标是尽量兼容不同 OpenAI 风格接口、中转站和自建服务，而不是绑定某一个具体服务商。

## 核心功能

- 支持任意 OpenAI 风格兼容站点
- 可配置 Base URL，例如：
  - `https://api.openai.com/v1`
  - `https://example.com/v1`
  - `https://your-domain.com/v1`
- 可配置 API Key
- 可分别配置文生图模型与图生图模型
- 支持三种接口模式：
  - `Images API`
  - `Responses API`
  - `Generations 图生图兼容`
- 文生图支持：
  - `POST /v1/images/generations`
- 图生图/图片编辑支持：
  - `multipart POST /v1/images/edits`
  - `POST /v1/responses`
  - 兼容部分中转站将图生图魔改到 `/v1/images/generations` 的实现
- 兼容返回：
  - `data[0].url`
  - `data[0].b64_json`
  - Responses API 图片输出结构
- 支持尺寸 / 比例预设选择，并保留独立画质参数
- 支持常见比例：
  - `1:1`
  - `3:2`
  - `2:3`
  - `4:3`
  - `3:4`
  - `16:9`
  - `9:16`
- 支持输出格式、背景、数量等高级参数
- 支持选择本地参考图
- 支持图片预览与保存到系统相册
- 支持简单本地历史记录
- 支持 Android 12+ Monet / Material You 动态色
- 支持正式签名 Release APK 发布

## 默认配置

默认 Base URL：

```text
https://api.openai.com/v1
```

你可以在 App 设置页中修改为任意兼容站点。

## 接口模式说明

### Images API

适合传统 OpenAI Images 接口：

```text
/v1/images/generations
/v1/images/edits
```

### Responses API

适合新版多模态统一接口，部分新模型或中转服务会优先支持：

```text
/v1/responses
```

### Generations 图生图兼容

用于兼容部分非标准服务商或中转站，它们可能把图生图也做在：

```text
/v1/images/generations
```

该模式会把参考图以兼容字段传入请求体，提升对非标准接口的适配范围。

## GitHub Actions 构建

项目已包含自动构建与发布流程：

```text
.github/workflows/android.yml
.github/workflows/release.yml
```

推送到 `main` 后会自动构建 APK；Release workflow 会发布正式签名 APK 到 GitHub Releases。

当前 workflow 已提前启用 Node 24 兼容验证：

```yaml
env:
  FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true
```

## 包名

```text
com.yang.emperor
```

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- Material You / Monet 动态色
- HttpURLConnection
- GitHub Actions
