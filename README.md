# ImageForge

[![Latest Release](https://img.shields.io/github/v/release/Wzindx/ImageForge?label=Latest%20Release)](https://github.com/Wzindx/ImageForge/releases/latest)

ImageForge 是一个面向 Android 的轻量图像生成工坊，基于 **Kotlin + Jetpack Compose + Material 3** 构建。它把图像生成、参考图编辑、接口配置、后台任务、应用内历史记录、图片分享/导出和移动端交互整合到一个适合手机使用的应用中。

README 面向项目整体功能与主分支能力进行说明。具体版本号、变更记录、APK 资产和发布时间请以 GitHub Releases 页面为准。

## 获取应用

```text
最新 APK：app-release.apk
下载地址：https://github.com/Wzindx/ImageForge/releases/latest
应用包名：com.yang.emperor
```

重点能力：

- 生成成功后**不会自动写入系统相册**。
- 生成结果会先保存到 App 内部记录，便于后续预览、打开和分享。
- 只有用户在详情页点击“保存”时，才会导出到系统相册或自定义保存目录。
- 历史记录不再依赖相册文件；删除相册中的图片不会影响 App 内部记录预览。
- 失败详情保留完整原始错误，支持滚动查看和复制完整错误。
- 网络层对 EOF、响应头读取失败、代理断流等问题加入自动重试与更明确提示。

## 项目定位

ImageForge 面向需要在手机端快速调用图像生成接口的用户，重点关注：

- 简单直接的创作入口
- 可配置的 Base URL / API Key / 模型 ID
- 文生图与参考图编辑流程
- 后台生成任务
- App 内部历史记录与结果管理
- 不强制自动下载到相册
- 移动端友好的参数选择、引导和错误排查

应用不会绑定某个固定服务商，而是通过 Base URL、API Key、接口模式和模型 ID 等配置，尽量兼容不同的图像生成服务端或中转接口。

## 功能总览

### 图像创作

- **文生图**：输入提示词后生成图像。
- **参考图 / 图生图**：选择参考图后，结合图片与提示词进行生成或编辑。
- **统一生成入口**：首页主按钮统一为“生成图像”，减少技术概念暴露。
- **后台执行**：点击生成后，任务可在后台继续执行。
- **完成通知**：生成完成或失败后可通过通知提示用户。

### 图片保存、分享与生命周期

默认行为：

1. 生成成功后，图片先保存到 App 私有目录：

   ```text
   files/generated_images
   ```

2. 历史记录保存 App 内部图片 URI。
3. 用户可以在详情页执行：
   - 打开图片
   - 保存图片
   - 分享图片
4. 只有点击“保存”时，才会导出到：
   - 系统相册
   - 或用户配置的自定义保存目录
5. 删除系统相册里的图片，不会影响 App 内历史记录的预览和分享。

这意味着 ImageForge 支持“不下载到相册也能分享”。

### 接口与模型

支持配置：

- Base URL
- API Key
- 接口模式
- 文生图模型 ID
- 图生图模型 ID

支持接口模式：

- Images API
- Responses API
- Generations 兼容模式

支持参数：

- 图片尺寸
- 画质
- 输出格式
- 背景模式
- 生成数量

Base URL 会自动处理 `/v1` 拼接：

- 如果 Base URL 已以 `/v1` 结尾，直接拼接接口路径。
- 如果没有 `/v1`，应用会自动补 `/v1`。

示例：

```text
https://example.com/v1
https://example.com
```

## 后台任务

- 点击“生成图像”后，任务会在后台继续执行。
- 切换到底部导航的历史页或设置页时，任务不会中断。
- 多个任务可以在历史记录中持续跟踪状态。
- 失败任务会保存详细错误，便于排查接口、模型、代理或参数问题。

## 历史记录页面

历史页用于集中查看生成任务：

- 处理中
- 成功
- 失败

### 外部列表

历史记录外部卡片只展示基础信息：

- 模型 ID
- 时间
- 类型：文生图 / 图生图
- 状态：处理中 / 处理成功 / 处理失败
- 失败项会显示简短摘要

外部列表不再放置操作按钮，避免页面拥挤。

### 详情页

点击历史记录卡片后进入详情弹窗。

成功记录详情支持：

- 查看 Prompt
- 长按选择复制 Prompt 文本
- 打开图片
- 保存图片
- 分享图片

失败记录详情支持：

- 滚动查看完整失败原因
- 复制完整错误详情
- 查看原始异常链和网络提示

### 错误信息完整性

历史记录不会再截断原始错误。

已经移除历史错误持久化层的截断逻辑：

```text
MAX_HISTORY_ERROR_CHARS
truncateHistoryError()
...（错误信息过长，已截断）
```

因此：

- 列表页只显示摘要。
- 详情页显示完整错误。
- “复制完整错误详情”复制完整原始错误。
- 历史记录重新打开后仍保留完整错误。

## 网络兼容与代理环境

ImageForge 对常见 VPN、代理、中转接口、网关不稳定问题做了兼容处理。

当前网络配置：

```text
connectTimeout = 30 秒
readTimeout = 180 秒
Connection: close
最大请求次数 = 3 次
```

当遇到以下常见问题时，应用会自动重试：

- `unexpected end of stream`
- `EOFException`
- 读取 HTTP 响应头失败
- `connection reset`
- `socket closed`
- `broken pipe`
- `premature end`
- `stream was reset`
- `timeout`
- `SocketException`
- `SocketTimeoutException`

重试策略：

- 首次请求失败后自动重试。
- 最多共尝试 3 次。
- 每次重试都会重新创建连接。
- 继续保留 `Connection: close`，降低连接复用在代理/中转场景下导致的断流概率。

如果多次重试仍失败，错误详情会明确提示可能原因：

- VPN / 代理节点不稳定
- 中转网关提前关闭连接
- Base URL 服务不稳定
- 目标接口返回空响应
- HTTP 长连接复用异常
- 当前网络抖动

建议排查方式：

1. 切换代理节点。
2. 关闭代理直接测试。
3. 开启代理后更换节点测试。
4. 更换更稳定的 Base URL。
5. 检查接口是否兼容所选 API 模式。
6. 复制完整错误详情用于排查。

## 引导与设置

应用提供首次引导和设置页中的“再来一次引导”入口，用于帮助用户配置接口信息。

引导流程包括：

- 项目用途介绍
- 能力说明
- 接口配置
- 开始使用

设置页集中管理连接配置，首页则尽量保持创作流程简洁。

## 移动端交互设计

ImageForge 的界面主要面向手机竖屏使用，交互重点是减少层级、降低误触和避免卡顿。

当前交互策略包括：

- 首页内容支持自然滚动。
- 接口、模型和生成参数使用底部面板承载。
- 参数选项使用对话框列表选择，避免在 BottomSheet 中嵌套复杂 Dropdown Popup。
- 选择参考图后先记录 Uri，真正生成时再读取和压缩图片，降低从系统相册返回应用时的卡顿。
- 通知权限不在启动时打扰用户，而是在生成相关流程中按需请求。
- 历史列表只展示记录概览，操作集中到详情弹窗。
- Prompt 使用文本选择方式复制，不再额外放置复制按钮。
- 失败详情可滚动，避免长错误撑爆页面。

## 使用流程

1. 从 GitHub Releases 下载 `app-release.apk`。
2. 安装并打开应用。
3. 首次启动时，根据引导填写：
   - Base URL
   - API Key
   - 模型 ID
4. 回到首页输入提示词。
5. 可选：选择参考图。
6. 点击“生成图像”。
7. 切换到历史页查看后台任务状态。
8. 生成成功后点击记录进入详情。
9. 在详情页选择：
   - 打开
   - 保存
   - 分享
10. 如果失败，进入详情页复制完整错误用于排查。

## 安装说明

请从 GitHub Releases 下载最新 APK：

```text
https://github.com/Wzindx/ImageForge/releases/latest
```

APK 文件：

```text
app-release.apk
```

应用包名：

```text
com.yang.emperor
```

如果 Android 提示“未知来源应用”，请按系统提示允许当前安装来源。

## 开发与构建

项目技术栈：

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin
- Gradle Wrapper
- GitHub Actions

主要文件：

```text
app/src/main/java/com/yang/emperor/MainActivity.kt
app/src/main/java/com/yang/emperor/MainUiComponents.kt
app/src/main/java/com/yang/emperor/HistoryStorage.kt
app/src/main/java/com/yang/emperor/ImageApiClient.kt
app/src/main/java/com/yang/emperor/AndroidImageActions.kt
app/src/main/res/xml/file_paths.xml
.github/workflows/android.yml
.github/workflows/release.yml
README.md
```

GitHub Actions 会自动执行 Android APK 构建，并在 Release 流程中上传发布资产。

### 开发者快速启动

推荐环境：

- Android Studio 最新稳定版
- JDK 17
- Android SDK / Build Tools 与项目 `compileSdk` 保持兼容
- Gradle Wrapper 8.14.4（仓库已提交 `gradlew`，推荐统一使用 Wrapper 构建）

常用命令：

```bash
# 构建 Debug APK
./gradlew :app:assembleDebug

# 构建 Release APK
./gradlew :app:assembleRelease

# 代码与资源静态检查
./gradlew :app:lintDebug
```

Windows 环境可使用：

```bash
gradlew.bat :app:assembleDebug
gradlew.bat :app:assembleRelease
```

### 当前稳定构建组合

当前主分支已验证的 CI 构建组合为：

```text
JDK: 17
Gradle Wrapper: 8.14.4
Android Gradle Plugin: 8.12.3
Kotlin: 2.3.21
```

请不要单独升级 Android Gradle Plugin 到 9.x。AGP 9.x 需要更高版本的 Gradle Wrapper；如果后续需要升级，应在单独分支中同时升级 Gradle Wrapper，并完整验证 lint、Debug APK、Release APK 和 Release workflow。

### Release 签名环境变量

Release 构建支持通过环境变量注入签名信息：

```bash
export ANDROID_KEYSTORE_PATH=/path/to/release.jks
export ANDROID_KEYSTORE_PASSWORD=***
export ANDROID_KEY_ALIAS=***
export ANDROID_KEY_PASSWORD=***
```

Debug 构建会使用独立包名后缀 `.debug`，避免误用 Release 签名和正式包名，方便在同一设备上并行安装调试版与正式版。

## 发布流程

当前发布流程：

1. 推送到 `main`。
2. GitHub Actions 执行 `Build Android APK`。
3. 构建成功后触发 `Release Android APK`。
4. Release workflow 覆盖刷新对应 Release 的 APK 资产。
5. 用户从 Release 页面下载最新 `app-release.apk`。

Release 页面：

```text
https://github.com/Wzindx/ImageForge/releases/latest
```

## 常见问题

### 生成后为什么相册里没有图片？

这是当前设计。

生成成功后图片默认保存在 App 内部历史记录中，不会自动写入系统相册。需要相册文件时，请进入记录详情页点击“保存”。

### 不保存到相册还能分享吗？

可以。

ImageForge 会通过 App 内部图片文件和 FileProvider 分享图片，不要求先保存到相册。

### 删除相册图片后，历史记录还能看吗？

可以。

历史记录使用 App 内部图片副本，不再依赖系统相册中的图片文件。

### 为什么失败详情很长？

失败详情会保留完整错误，便于排查接口、代理、模型和参数问题。列表页只显示摘要，完整内容在详情页中滚动查看或复制。

### 出现 unexpected end of stream / EOFException 怎么办？

这通常和代理节点、中转网关、Base URL 或服务端稳定性有关。应用会自动重试这类断流问题。如果最终仍失败，请尝试：

- 切换代理节点
- 更换 Base URL
- 检查所选接口模式
- 复制完整错误详情继续排查

## 参考与致谢

本项目在产品体验、界面风格和文档组织上参考与借鉴了以下开源项目。感谢这些项目及其开发者提供的启发。

### compose-miuix-ui / miuix

项目地址：

```text
https://github.com/compose-miuix-ui/miuix
```

`compose-miuix-ui/miuix` 是一个面向 Compose Multiplatform 的 UI library，提供 MiuixTheme、miuix-ui、miuix-preference、miuix-icons 等模块。

ImageForge 参考了其所呈现的 Miuix / HyperOS 风格方向，包括：

- 更大的圆角
- 柔和的浅色卡片
- 清晰的分组层级
- 移动端优先的视觉节奏

说明：

- 当前项目未直接包含 `compose-miuix-ui/miuix` 的源码。
- 当前 Gradle 配置未将 `top.yukonga.miuix.kmp:miuix-ui` 作为依赖引入。
- README 中提到 miuix 仅表示视觉与体验方向上的参考和致谢。
- 本项目不是 `compose-miuix-ui/miuix` 的官方项目或官方衍生版本。

### ReChronoRain / HyperCeiler

项目地址：

```text
https://github.com/ReChronoRain/HyperCeiler
```

`HyperCeiler` 是面向 Xiaomi HyperOS / MIUI 生态的开源项目，其 README 中的项目说明、使用前说明、感谢与引用项目结构非常清晰。

ImageForge 参考了其部分表达方式和体验组织思路，包括：

- 更直接的项目口号式表达
- 首次使用前的说明结构
- 将参考项目和开源致谢单独列出
- 面向用户说明限制、来源和关系的写法

说明：

- 当前项目未直接包含 `HyperCeiler` 的源码。
- 当前项目不是 Xposed / LSPosed 模块。
- 当前项目不修改系统，也不属于 HyperCeiler 官方生态。
- README 中提到 HyperCeiler 仅表示文档结构、引导体验和开源致谢表达上的参考。

### 其他参考

本项目的图像生成工作流、模型参数组织和轻量化 Playground 体验，也参考过以下项目的设计方向：

```text
https://github.com/CookSleep/gpt_image_playground
```

说明：

- 当前项目为 Android Kotlin / Jetpack Compose 实现。
- 未在源码中保留该项目的直接代码引用或依赖痕迹。

## 关系声明

ImageForge 是独立项目。

除非特别说明，本项目与上述参考项目的作者、组织或社区不存在官方从属关系、合作关系或背书关系。所有项目名称和链接仅用于说明参考来源、学习方向和开源致谢。

## License

本项目采用 Apache License 2.0 授权，详见仓库根目录的 `LICENSE` 文件。

除非特别说明，项目中提到的第三方项目名称、链接、商标和服务仍归其各自权利人所有。
