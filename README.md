<p align="center">
  <img src="design/wraive-icon.png" width="112" height="112" alt="Wraive 图标" />
</p>

# Wraive

为 Wear OS 打造的 AI 助手，在手腕上开启文字与语音对话。

Wraive 使用 Kotlin 与 Jetpack Compose 构建，围绕手表的圆形屏幕、旋转表冠与底部边缘按钮设计交互。连接你选择的模型服务，即可在手表上提问、翻译、使用工具并管理会话。

[项目仓库](https://github.com/youshen2/Wraive) · [问题反馈](https://github.com/youshen2/Wraive/issues)

## 功能

- **模型与助手**：支持 OpenAI Chat Completions、OpenAI Responses、Anthropic 和 Google 接口；按供应商管理模型，自定义助手与生成参数。
- **文字与语音**：流式回复、语音输入、回复朗读、Markdown 与 LaTeX 公式显示，以及图片和文档附件；AI 回复显示助手名称。
- **搜索与工具**：联网搜索、MCP 工具、长期记忆、世界书、提示词转换和翻译工作台。
- **会话管理**：标题编辑、置顶、归档、临时对话、全局搜索，以及 Markdown / HTML 导出。
- **备份与恢复**：本地备份、S3 兼容云端加密备份，以及供应商配置的二维码分享与导入。
- **手表体验**：圆屏适配、表冠滚动、内置文件选择器、原生 Wear Material 3 组件、动态配色、自定义字体和中英文界面。

## 开始使用

1. 从首页底部菜单进入 **设置 → 模型与助手 → 模型供应商**，添加供应商，填写接口类型、服务地址和 API Key。
2. 打开对应的供应商详情页，拉取模型列表或手动添加模型。
3. 在 **设置 → 模型与助手 → 助手** 中创建或编辑助手，为其选择模型。
4. 回到首页，通过底部菜单创建新对话或临时对话。

搜索、MCP、语音和云备份服务可在设置中按需配置。

连接 DeepSeek 官方 API（`api.deepseek.com`）时，`deepseek-flash` 及其官方兼容别名默认启用联网搜索和链接读取，由 DeepSeek 服务端执行，并保留引用来源与多轮网页上下文。`deepseek-v4-pro` 使用 Responses 或 Anthropic 协议时默认启用官方联网搜索。能力与接入格式见 [DeepSeek Web Tools](https://api-docs.deepseek.com/guides/web_tools/)、[Responses](https://api-docs.deepseek.com/guides/responses_api/) 和 [Anthropic](https://api-docs.deepseek.com/guides/anthropic_api/) 文档。

## 构建与安装

### 环境

- JDK 21。
- Android SDK Platform 37，以及 Android SDK Build-Tools、Platform-Tools。
- Android Studio 或命令行环境；使用仓库内的 Gradle Wrapper。
- Wear OS 手表或模拟器，应用最低 Android API 为 26。

### 构建调试版本

```bash
git clone --recurse-submodules https://github.com/youshen2/Wraive.git
cd Wraive
./gradlew :app:assembleDebug
```

使用 Android Studio 打开项目时可由 IDE 配置 SDK 路径。命令行构建可通过 `ANDROID_HOME` 指定 SDK，或在本地 `local.properties` 中填写 `sdk.dir`。

已有工作副本先运行 `git submodule update --init --recursive`，以获取构建所需的 CN Wear Overlay 插件。

Windows 下使用 `gradlew.bat :app:assembleDebug`。调试 APK 输出至：

```text
app/build/outputs/apk/debug/app-debug.apk
```

开启手表的开发者选项和调试功能，完成 ADB 连接后安装：

```bash
adb devices
adb -s <device-serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

### 验证

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebugAndroidTest
```

界面验证用例位于 `app/src/androidTest`，可在 Android Studio 中选择目标 Wear OS 模拟器运行。

## 项目结构

```text
app/src/main/java/moye/wear/wraive/
├── ui/          界面、导航、组件与主题
├── chat/        会话生成、提示词与工具调度
├── network/     模型服务、搜索、MCP 与网络连接
├── data/        数据存储、导入导出与备份
├── audio/       语音输入与朗读
├── model/       数据模型与配置
└── service/     后台生成服务

app/src/main/res/    Android 资源与自适应图标
```

## 作者与致谢

设计与开发：**爅峫**。

本项目参考了 [Kelivo](https://github.com/Chevey339/kelivo)，感谢其带来的启发。

通过 [CN Wear Overlay](https://github.com/Star-ZER0/cnwearoverlay) 的 Gradle 插件适配小米 / OPPO 手表的表冠触觉与小米触觉常量；小米 Watch 5（M2505W1、M2501W1）使用系统返回导航。接入方式参考 [Orbit](https://github.com/PianoEthan/Orbit)。

## 许可证

Copyright (C) 2026 爅峫。

本项目采用 [GNU Affero General Public License v3.0](LICENSE) 发布，SPDX 标识为 `AGPL-3.0-only`。完整条款见 [LICENSE](LICENSE)。
