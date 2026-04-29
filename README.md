# UniversalImageStudio

Android 图像生成工具，当前主线版本为 **v1.8**，包名为：

```text
com.yang.emperor
```

## 当前版本

- 最新发布：`v1.8`
- 安装包：前往 GitHub Release 下载 `app-release.apk`
- 旧版 `v1.7` 已下架，避免下载到旧包名版本

## 功能特性

- 文生图：输入提示词生成图像
- 图生图：选择参考图后生成新图像
- 兼容接口：支持 Images / Responses / Generations 兼容模式
- 后台任务：点击“生成图像”后任务在 Activity 生命周期内继续运行，切到历史或设置页不会中断
- 历史记录：展示处理中、成功、失败状态
- 首次引导：第一次打开引导填写接口信息，设置页可再次打开引导
- 首页滚动：接口与模型、生成参数等区域可自然滑动隐藏

## v1.8 重点更新

- 修复兼容接口文生图切换页面后出现 `The coroutine scope left the composition` 的问题
- 后台生成从 Compose 页面作用域改为生命周期级后台任务
- 按钮统一显示“生成图像”
- 优化生成按钮反色与禁用态对比
- 包名切换为 `com.yang.emperor`
- 精简发布说明并清理旧版本入口

## 使用说明

1. 安装 Release 页面下方的 `app-release.apk`
2. 首次打开时按引导填写接口地址、API Key 和模型
3. 回到首页输入提示词
4. 点击“生成图像”
5. 可切到历史页查看后台任务状态

## 开发构建

```bash
# GitHub Actions 会自动构建 APK
# 本地若有 Gradle 环境，可执行：
gradle :app:assembleRelease
```

## 安全提醒

请不要把 GitHub Token、API Key 等密钥提交到仓库或发布说明中。
