# Aetheria Reader

A premium, 100% offline local Book & Comic reader app for Android, focusing on an exquisite reading experience and absolute privacy. 

This app operates entirely offline with zero network permissions, zero ads, and no registration required. The visual design focuses on bringing the warmth and tactile comfort of traditional paper into the digital reading realm.

---

## ✨ Features

### 🎨 Literary Aesthetics & Eye Care
* **Light Mode (Warm Paper):** A carefully tuned, high-textured warm cream/paper background (`#F9F6F0`) paired with soft ink-black text and muted terracotta accents, replicating the comforting feel of reading a physical book.
* **Dark Mode (Midnight Ink):** A deep midnight-ink tone optimized for long immersive reading sessions at night, protecting your eyes without sacrificing style.

### 📦 Robust Local Multi-Format Parsers
* **TXT:** Non-blocking streaming parser powered by Kotlin Coroutines. It intelligently detects chapters using Regex for both Chinese (e.g., *“第一百二十章”*) and English (e.g., *“Chapter 120”*). Utilizes **byte-offset chunk loading** to open 20MB+ files instantly without RAM overflow risks.
* **EPUB:** Seamlessly extracts standard EPUB structures, embedded styles (CSS), and book covers.
* **PDF:** High-definition vector rendering utilizing native engines, supporting smooth pinching and zooming.
* **RAR (Comics):** Decompresses image streams locally on-the-fly, supporting continuous vertical scrolling (infinite waterfall mode) preferred by comic lovers.

### 🔒 Absolute Privacy
* No internet permission requested. Your data remains strictly within the local Room Database, isolated securely via Android's Scoped Storage and Storage Access Framework (SAF).

### 🌐 Internationalization
* **Default:** Chinese (简体中文 / zh-CN)
* **Supported:** English (en), Japanese (日本語 / ja)

---

## 🛠 Tech Stack

* **Language:** Kotlin (1.9+)
* **UI Framework:** Jetpack Compose + Material 3 (Reactive, declarative modern UI)
* **Asynchrony:** Kotlin Coroutines & Flow
* **Local Database:** Jetpack Room Database (For book metadata, chapter indexing, and reading progress)
* **Architecture:** MVVM + Clean Architecture

---

## 📸 Screenshots

| Light Mode (Warm Paper) | Dark Mode (Midnight Ink) |
| --- | --- |
| *[Insert Light Mode Screenshot]* | *[Insert Dark Mode Screenshot]* |

---

## 🚀 Quick Start

### Requirements
* Android Studio Jellyfish (2024.1.1) or higher
* Gradle 8.4+
* Compile SDK: 34+
* Min SDK: 26 (Android 8.0+)

### Setup
```bash
# Clone the repository
git clone [https://github.com/zfy273/AetheriaReader.git](https://github.com/zfy273/AetheriaReader.git)

# Navigate to the project directory
cd AetheriaReader

```

Open the project with Android Studio, wait for Gradle sync to complete, and click `Run` to deploy it to your device.

---

## 📅 Roadmap

* [x] Multi-language localization support (zh, en, ja)
* [x] Material 3 typographic theme configuration
* [x] Room Database architecture design
* [ ] Regex & byte-stream efficient TXT split engine
* [ ] EPUB/PDF/RAR local parsers implementation
* [ ] Realistic canvas page-flip turning animation

---

## 📄 License

This project is licensed under the [MIT License](https://www.google.com/search?q=LICENSE).

================================================================================

# 苍缈阅读 (Aetheria Reader)

一款专注于极致本地阅读体验与隐私安全的 Android 小说/漫画阅读器。

本软件采用 100% 纯本地架构，无任何网络请求，无广告，无需注册。视觉风格旨在为泛黄的纸张注入现代数字阅读的温度，还原纸质阅读的触感与舒适度。

---

## ✨ 核心特性

### 🎨 拟物美学与护眼体验

* **浅色模式（人文纸张）：** 精心调配的暖米色/高质感牛皮纸背景（`#F9F6F0`），搭配柔和的墨黑文字与复古铁锈红点缀，重现纸质书的人文质感。
* **深色模式（午夜水墨）：** 深邃的午夜水墨色调，专为夜间长途阅读优化，护眼而不失格调。

### 📦 强大的纯本地解析

* **TXT：** 基于协程的非阻塞流式解析，智能正则匹配中英文章节（如 *“第一百二十章”* / *“Chapter 120”*）。采用**字节偏移量分块加载**技术，20MB+ 大文件秒开，零内存溢出风险。
* **EPUB：** 完整解析标准电子书的 OPF 结构、内嵌样式与书籍封面。
* **PDF：** 基于原生引擎的矢量级高清渲染，支持流畅缩放。
* **RAR（漫画）：** 本地解压图片流，支持漫画迷最爱的**连续纵向瀑布流滚动**（无限瀑布流模式）。

### 🔒 绝对隐私安全

* 不申请任何网络权限。数据全在本地 Room 数据库中，支持系统沙盒与 SAF（存储访问框架）隔离。

### 🌐 国际化支持

* **默认：** 简体中文 (zh-CN)
* **已支持：** English (en), 日本語 (ja)

---

## 🛠 技术栈

* **开发语言：** Kotlin (1.9+)
* **UI 框架：** Jetpack Compose + Material 3 (响应式、声明式现代 UI)
* **异步与流：** Kotlin Coroutines & Flow
* **本地数据库：** Jetpack Room Database (管理书籍元数据、章节索引及阅读进度)
* **架构模式：** MVVM + Clean Architecture

---

## 📸 视觉预览

| 浅色模式 (人文纸张) | 深色模式 (午夜水墨) |
| --- | --- |
| *[请在此处替换为你的应用截图]* | *[请在此处替换为你的应用截图]* |

---

## 🚀 快速开始

### 环境要求

* Android Studio Jellyfish (2024.1.1) 或更高版本
* Gradle 8.4+
* Compile SDK: 34+
* Min SDK: 26 (Android 8.0+)

### 克隆与编译

```bash
# 克隆本项目
git clone [https://github.com/zfy273/AetheriaReader.git](https://github.com/zfy273/AetheriaReader.git)

# 进入项目目录
cd AetheriaReader

```

用 Android Studio 打开项目，等待 Gradle 同步完成后即可点击 `Run` 运行至你的 Android 手机。

---

## 📅 开发计划

* [x] 多语言本地化支持 (zh, en, ja)
* [x] 人文质感主题配置配置
* [x] Room 数据库架构设计
* [ ] 高效 TXT 章节拆分引擎
* [ ] EPUB/PDF/RAR 本地解析器实现
* [ ] 仿真纸张翻页效果

---

## 📄 开源协议

本项目采用 [MIT License](https://www.google.com/search?q=LICENSE) 协议开源。

```

```
