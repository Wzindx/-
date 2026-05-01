# ImageForge

[![Latest Release](https://img.shields.io/github/v/release/Wzindx/ImageForge?label=Latest%20Release)](https://github.com/Wzindx/ImageForge/releases/latest)

ImageForge 是一个面向 Android 的通用图像生成工坊，基于 Kotlin 与 Jetpack Compose 构建。项目目标是把图像生成、参考图编辑、接口配置、后台任务、历史记录和移动端交互整合到一个轻量、直接、适合手机使用的应用中。

本项目不是某个单一版本的更新日志，而是一个持续维护的 Android 图像创作工具。README 主要说明项目整体定位、功能、使用方式、构建方式以及参考与致谢信息。

## 项目定位

ImageForge 面向需要在手机端快速调用图像生成接口的用户，重点关注：

- 简单直接的创作入口
- 可配置的接口与模型
- 文生图与参考图编辑流程
- 后台生成任务
- 历史记录与结果管理
- 更适合移动端的参数选择和引导体验

应用不会强行绑定某个固定服务商，而是通过 Base URL、API Key、接口模式和模型 ID 等配置，尽量兼容不同的图像生成服务端。

## 功能总览

### 图像创作

- **文生图**：输入提示词后生成图像。
- **参考图 / 图生图**：选择参考图后，结合图片与提示词进行生成或编辑。
- **统一生成入口**：首页主按钮统一为“生成图像”，减少技术概念暴露。
- **结果预览**：生成完成后可在应用内查看结果。
- **相册保存**：生成成功后可保存到系统相册。
- **完成通知**：生成完成后可通过通知栏提示用户。

### 接口与模型

- 支持配置：
  - Base URL
  - API Key
  - 接口模式
  - 文生图模型 ID
  - 图生图模型 ID
- 支持多种接口模式，便于适配不同服务端：
  - Images API
  - Responses API
  - Generations 兼容模式
- 支持调整：
  - 图片尺寸
  - 画质
  - 输出格式
  - 背景模式
  - 生成数量

### 后台任务

- 点击“生成图像”后，任务会在后台继续执行。
- 切换到底部导航的历史页或设置页时，任务不会中断。
- 多个任务可以在历史记录中持续跟踪状态。
- 任务失败时会保留错误信息，便于排查接口、模型或参数问题。

### 历史记录

历史页用于集中查看生成任务：

- 处理中
- 成功
- 失败

成功任务支持：

- 查看结果
- 保存图片
- 分享图片

失败任务会展示错误信息，便于用户调整配置后重试。

### 引导与设置

应用提供首次引导和设置页中的“再来一次引导”入口，用于帮助用户配置接口信息。

引导流程重点包括：

- 项目用途介绍
- 能力说明
- 接口配置
- 开始使用

设置页集中管理连接配置，首页则尽量保持创作流程简洁，避免长期显示过多配置提示。

## 移动端交互设计

ImageForge 的界面主要面向手机竖屏使用，交互设计重点是减少层级和避免卡顿。

当前交互策略包括：

- 首页内容支持自然滚动。
- 接口、模型和生成参数使用底部面板承载。
- 参数选项使用对话框列表选择，避免在 BottomSheet 中嵌套复杂 Dropdown Popup。
- 选择参考图后先记录 Uri，真正生成时再读取和压缩图片，降低从系统相册返回应用时的卡顿。
- 通知权限不在启动时打扰用户，而是在生成相关流程中按需请求。
- 字体使用系统 SansSerif 风格，尽量保持不同设备上的阅读一致性。

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
7. 可切换到历史页查看后台任务状态。
8. 生成成功后查看、保存或分享图片。

## 安装说明

请从 GitHub Releases 下载最新 APK：

```text
app-release.apk
```

应用包名：

```text
com.yang.emperor
```

当前项目版本号由 Android Gradle 配置中的 `versionName` 决定。

## 开发与构建

项目技术栈：

- Kotlin
- Jetpack Compose
- Material 3
- Android Gradle Plugin
- GitHub Actions

本地具备 Gradle / Android 构建环境时，可执行：

```bash
gradle :app:assembleRelease
```

主要文件：

```text
app/src/main/java/com/yang/emperor/MainActivity.kt
app/src/main/java/com/yang/emperor/MainUiComponents.kt
app/src/main/java/com/yang/emperor/ui/theme/
.github/workflows/android.yml
.github/workflows/release.yml
README.md
```

GitHub Actions 会自动执行 Android APK 构建，并在 Release 流程中覆盖上传发布资产。

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

本仓库已提交 Gradle Wrapper。请优先使用 `./gradlew`，不要依赖本机全局 Gradle 版本，以保证本地与 CI 构建环境一致。Windows 环境可使用 `gradlew.bat`。

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
