# 生活常识 LifeStudy

一个 Android 原生 App（Kotlin + Jetpack Compose），用于认识身边的生活常识：

- 🐶 狗的种类（12 个常见犬种）
- 🐱 猫的种类（12 个常见品种）
- 🚗 车的型号（12 款代表车型）
- 🍎 水果的种类（12 种常见水果）
- 🥬 蔬菜的种类（12 种常见蔬菜）
- 🌸 花的种类（12 种常见花卉）

每一项都包含：中文名、英文名、特征标签、简介、在线缩略图。

## 工程结构

```
life-study/
├── settings.gradle.kts         # Gradle 模块声明
├── build.gradle.kts            # 项目级构建文件（AGP / Kotlin 版本）
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── app/
    ├── build.gradle.kts        # 应用级构建（依赖、签名、SDK 版本）
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/lifestudy/app/
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── Models.kt           # Category / Item 数据类
        │   │   └── DataSource.kt       # 6 个分类的全部数据
        │   └── ui/
        │       ├── nav/AppNav.kt       # Navigation Compose 路由
        │       ├── theme/              # 主题、配色、字号
        │       └── screens/
        │           ├── HomeScreen.kt       # 首页：6 个分类卡片网格
        │           ├── CategoryScreen.kt   # 分类页：列表 + 搜索过滤
        │           └── DetailScreen.kt     # 详情页：大图 + 标签 + 描述
        └── res/                          # 资源（图标、颜色、字符串）
```

## 技术栈

- Kotlin 1.9.24
- AGP 8.4.2 / Gradle 8.7
- Jetpack Compose（Material 3，BOM 2024.06）
- Navigation Compose
- Coil（在线图片加载）
- Compile/Target SDK 34，Min SDK 24

## 如何打包出 .apk 文件

本机当前**没有安装 Android SDK**，需要先准备好开发环境，然后任选一种方式打包。

### 方式一：Android Studio（推荐）

1. 下载 [Android Studio](https://developer.android.com/studio) 并安装。
2. 启动后选择 **Open**，打开本目录 `life-study/`。
3. 等待 Gradle Sync 自动完成（首次会下载 SDK 与依赖）。
4. 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**。
5. 构建完成后，点击通知里的 **locate** 或到 `app/build/outputs/apk/debug/app-debug.apk` 取出 APK。

### 方式二：命令行

```bash
# 安装 Android SDK（命令行工具）
# 例：通过 sdkmanager 安装 platform-tools / platforms;android-34 / build-tools;34.0.0

# 在项目根目录初始化 wrapper
gradle wrapper --gradle-version 8.7

# 配置 SDK 路径
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# 打包 debug APK
./gradlew assembleDebug

# 产物位置：
# app/build/outputs/apk/debug/app-debug.apk
```

> 如需 release 包，需要先准备 keystore 并在 `app/build.gradle.kts` 添加 signingConfigs，然后 `./gradlew assembleRelease`。

## 网络与图片

- 应用使用 [loremflickr.com](https://loremflickr.com) 按关键词从 Flickr 拉取真实照片；首次进入分类需要联网。
- 如果想换成自己准备的图片，把 `DataSource.kt` 里每条 `imageUrl` 改成对应的 URL 即可，无需改动其他代码。
- 完全离线场景：把图片放入 `app/src/main/res/drawable/`，将 `imageUrl: String` 改为资源 ID，或在 `AsyncImage(model = ...)` 处替换为 `painterResource(...)`。

## 二次开发指引

- **新增一个分类**：在 `DataSource.kt` 里仿照已有的 `dogs/cats/...` 定义一个 `Category`，加进底部的 `categories` 列表即可，UI 自动出现新卡片。
- **新增一条数据**：在对应 `Category` 的 `items = listOf(...)` 里追加 `Item(...)`。
- **修改主题色**：编辑 `ui/theme/Color.kt`；单个分类的强调色在 `DataSource.kt` 各 `Category` 的 `accent` 字段。
- **加入收藏 / 历史**：建议引入 DataStore Preferences；当前实现纯内存、无持久化。
