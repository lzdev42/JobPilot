# JobPilot

**一个基于 Kotlin、Compose for Desktop 和 Playwright 构建的现代化、图形化自动化求职应用。**

[![最新版本](https://img.shields.io/github/v/release/your-username/JobPilot?label=最新版本&logo=github)](https://github.com/your-username/JobPilot/releases/latest)
[![下载量](https://img.shields.io/github/downloads/your-username/JobPilot/total.svg?label=下载量&logo=github)](https://github.com/your-username/JobPilot/releases)
[![Kotlin 版本](https://img.shields.io/badge/Kotlin-2.1.21-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-1.8.0-4285F4.svg?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Playwright 版本](https://img.shields.io/badge/Playwright-1.52.0-2EAD33.svg?logo=microsoft)](https://playwright.dev/java/)
[![许可证](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## 注意：不要高估AI的智商，更不要高估谷歌 AI 的智商

## 概述

JobPilot 是一款独立的桌面应用程序，旨在通过自动化技术简化并高效化在线求职流程。它提供了一个直观的图形用户界面（GUI），用户无需编辑任何配置文件，即可方便地设置和管理自动化任务。

应用后端基于 Kotlin 协程和 Playwright 构建，确保了在复杂的网络环境中进行浏览器自动化操作时的高性能和高稳定性。

## ⚠️ 项目状态

**当前版本已稳定支持 `Boss直聘` 平台，并集成了 AI 功能以优化投递体验。** 其他平台的适配工作正在进行中。

| 平台 | 支持状态 | 备注 |
| :--- | :--- | :--- |
| **Boss直聘** | ✅ **已支持** | 功能完整，支持 AI 生成个性化招呼语。 |
| **前程无忧(51job)** | 🚧 **开发中** | 基础框架已搭建，功能正在完善。 |
| **猎聘** | 规划中 | |

## ✨ 核心特性

*   **图形化操作界面**: 所有配置项，包括搜索关键词、目标城市及 AI 模型设置，均通过简洁的 UI 进行管理，提供了“开箱即用”的用户体验。
*   **AI 智能辅助**: **集成 Google Gemini API**，可根据用户 简历概述 和 职位描述（JD）自动判断工作是否匹配，以及生成个性化的问候语，显著提高简历回复率。
*   **独立桌面应用**: 可自行打包成适用于 Windows (`.msi`)、macOS (`.dmg`) 和 Linux (`.deb`) 的原生应用。
*   **现代化异步核心**: 整个自动化引擎基于 Kotlin 协程和 Playwright 构建，以非阻塞模式处理所有浏览器交互，保证了任务执行的流畅性和高效率。
*   **可扩展架构**: 采用清晰的模块化设计，方便社区贡献者在现有框架基础上扩展对新招聘平台或新 AI 服务的支持。
*   **实时日志视图**: 应用界面内嵌日志面板，用户可实时监控自动化任务的每一个步骤和状态。

## 🤖 AI 功能说明

本应用的 AI 功能由 **Google Gemini** 提供支持。

*   **功能**：在投递 Boss 直聘职位时，程序会自动获取职位描述（JD），并将其发送给 Gemini 模型，以生成一段与该职位高度匹配的、个性化的招呼语。
*   **如何启用**：
    1.  访问 [**Google AI Studio**](https://aistudio.google.com/app/apikey) 获取您的免费 API Key。
    2.  将获取到的 API Key 填入 JobPilot 应用的设置界面中。
*   **关于费用**：Google Gemini API 目前提供非常慷慨的免费使用额度，对于本应用的日常使用量来说通常是完全免费的。详情请参阅 [Google官方定价策略](https://ai.google.dev/pricing)。
*   **未来支持**：本项目的架构设计支持快速扩展至其他 AI 模型服务（如 OpenAI/ChatGPT 等）。
    > 👉 **[点击阅读：AI求职助手配置与使用指南](./AI_PROMPT_GUIDE.md)** 👈

## 👨‍💻 面向开发者

我们欢迎对本项目感兴趣的开发者参与贡献或从源码构建。

### 环境要求

*   **JDK 21**
*   **Kotlin 2.0.0**
*   **IntelliJ IDEA** (推荐)


## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。
