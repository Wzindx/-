# UniversalImageStudio

UniversalImageStudio 是一个面向 Android 的通用图像生成工坊，整合文生图、图生图、兼容接口调用、后台任务、历史记录、首次配置引导和移动端交互优化。

## 功能总览

### 图像创作

- **文生图**：输入提示词后生成图像。
- **图生图**：选择参考图后，结合图片和提示词生成新图像。
- **统一生成入口**：首页主按钮统一显示为“生成图像”，不向用户暴露队列、编辑等技术实现。
- **结果预览**：生成完成后可在应用内查看结果。
- **按需下载**：生成结果不强制自动保存，用户可按需下载。

### 接口与模型

- 支持配置 Base URL、API Key 和模型名称。
- 支持在首页快速调整接口模式、模型和生成参数。
- 支持多种接口模式，便于适配不同服务端：
  - Images API
  - Responses API
  - Generations 兼容模式
- 支持调整尺寸、比例、画质、输出格式、背景、生成数量等参数。

### 后台任务

- 点击“生成图像”后，任务会在后台继续执行。
- 后台任务不绑定单个 Compose 页面，切换到底部导航其他页面时不会中断。
- 任务运行期间可继续查看历史、调整设置或准备下一次创作。
- 兼容接口文生图在切换页面后也能保持任务执行。

### 历史记录

- 历史页展示任务状态：
  - 处理中
  - 成功
  - 失败
- 失败任务会保留错误信息，方便排查接口、模型或参数问题。
- 成功任务可结合结果预览继续查看或下载。

### 首次引导与设置

- 首次打开应用时，如果接口信息未配置，会进入引导页。
- 设置页提供“再来一次引导”，方便重新配置。
- 首页不常驻显示配置提示，减少对创作流程的干扰。
- 设置页集中管理连接配置，首页聚焦图像创作。

### 首页与移动端交互

- 首页创作内容支持滚动，接口与模型、生成参数等区域可自然滑动。
- 底部导航选中态轻量化，减少大色块压迫感。
- 生成按钮优化了高亮、反色和禁用态对比。
- 接口与模型、生成参数等底部面板采用半屏弹窗：
  - 不占满整个屏幕
  - 内容内部可滚动
  - 支持下滑关闭
  - 点击空白区域也可关闭
- 下拉菜单在栏下方自然弹出，内容过多时限制高度并支持滚动。
- 参考图读取流程已优化，大图会采样处理以减少选图瞬间卡顿。

## 使用流程

1. 安装 Release 页面中的 `app-release.apk`。
2. 首次打开时，根据引导填写：
   - Base URL
   - API Key
   - 模型名称
3. 回到首页输入提示词。
4. 可选：选择参考图。
5. 点击“生成图像”。
6. 可切到历史页查看后台任务状态。
7. 生成成功后在结果预览中查看或下载图片。

## 安装说明

请从 GitHub Release 下载最新版本安装包：

```text
app-release.apk
```

应用包名：

```text
com.yang.emperor
```

## v1.9 更新说明

- 全面升级 Kotlin 2.3.20、AGP 8.12.1、Compose BOM 2026.04.00，并正式接入 `compose-miuix-ui/miuix` 的 `miuix-ui:0.9.0`。
- AppTheme 已切换为 `MiuixTheme` 包裹的 Compose 主题结构，保留 Material3 组件兼容层，降低一次性迁移风险。
- UI 调整为 MIUIX 风格：更大的圆角、浅色分组卡片、柔和蓝紫主色和更清晰的层级。
- 新增参考 HyperCeiler 首次体验思路的 OOBE 引导页，包含欢迎、能力说明、接口配置和开始使用。

## 参考与说明

本项目的产品思路参考了 [CookSleep/gpt_image_playground](https://github.com/CookSleep/gpt_image_playground) 中关于图像生成工作流、模型参数组织和轻量化 Playground 体验的设计方向。

当前项目为 Android Kotlin / Jetpack Compose 实现，未在源码中保留该项目的直接代码引用或依赖痕迹。

## 开发与构建

项目为 Android Kotlin / Jetpack Compose 应用。

GitHub Actions 会自动构建 Release APK。  
本地环境具备 Gradle 时可执行：

```bash
gradle :app:assembleRelease
```

主要文件：

```text
app/src/main/java/com/yang/emperor/MainActivity.kt
.github/workflows/android.yml
.github/workflows/release.yml
README.md
```
