# 📚 阅读器 (Local Reader)

一款精美、流畅的本地电子书与漫画阅读器。支持多种格式，专为提供沉浸式的阅读体验而设计。

## ✨ 核心功能

- 📖 **多格式支持**：全面支持 TXT 纯文本、PDF 文档以及 ZIP/CBZ 漫画格式。
- 🎨 **精美 UI 设计**：采用 Material Design 3 设计规范，配合流畅的弹簧微动画（如卡片按压回弹），带来丝滑的操作体验。
- 🌗 **护眼主题**：提供“纸质暖白”与“水墨深夜”等多种阅读背景，适应不同光线环境。
- 🌍 **多语言切换**：内置灵活的本地化支持，无需重启应用即可即时切换语言（支持中文、英文等）。
- 📂 **本地书架管理**：可以轻松导入本地书籍，自动分析提取书籍基本信息与阅读进度。
- 🔖 **手势与快速翻页**：支持页面点击及滑块快速定位，让阅读不间断。

## 🚀 获取与编译 APK

本应用基于现代 Android 架构开发 (Kotlin + Jetpack Compose)。

### 获取 APK 文件的路径
如果您在本地电脑上使用 **Android Studio** 或命令行自行编译本代码：
1. 请在项目根目录下打开终端，运行：
   ```bash
   ./gradlew assembleDebug
   ```
2. 编译成功后，APK 文件的相对存放路径为：
   **`app/build/outputs/apk/debug/app-debug.apk`**
   （您可以在文件资源管理器中按照这个路径找到生成的安装包）

## 🛠 技术架构

- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM (Model-View-ViewModel)
- **异步处理**: Kotlin Coroutines & StateFlow
