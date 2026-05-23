# 生活常识 LifeStudy

一个 Android 原生 App（Kotlin + Jetpack Compose），用卡片化方式认识身边的常识。
内置 10 个分类、120+ 条目，并支持自定义新分类 / 新条目、上传自己的图片、打卡"已认识"。

## 截图速览（功能层面）

- 首页：分类卡片 2 列网格，长按任意卡片进入排序模式，可拖动调整顺序
- 分类页：搜索过滤 + 网格 / 列表展示，每个条目右上角有「已认识」勾标
- 详情页：大图 + 中英文名 + 标签 + 描述；可上传封面图覆盖、追加最多 3 张额外图
- 右下角 FAB：新增分类 / 新增条目

## 内置分类

| Emoji | 分类 | 条目数 |
| --- | --- | ---: |
| 🐶 | 狗的种类 | 12 |
| 🐱 | 猫的种类 | 12 |
| 🚗 | 车的型号 | 12 |
| 🌸 | 花的种类 | 12 |
| 💊 | 药的种类 | 12 |
| 🏛️ | 建筑风格 | 12 |
| 🍵 | 茶的种类 | 12 |
| 🍶 | 酒的品牌 | 24 |
| 🍜 | 美食的种类 | 12 |
| 🦸 | 奥特曼 | 12 |

每条数据包含：中文名、英文名、特征标签、简介、缩略图。

> 历史版本曾内置 `fruits / veggies / fishes / birds / planes` 等分类，现已下线，升级时会被自动清理（用户自己添加的分类不受影响）。

## 技术栈

- Kotlin **1.9.24** / AGP **8.4.2** / Gradle **8.7**
- Jetpack Compose（Material 3，BOM 2024.06）
- Navigation Compose 2.7.7
- Coil 2.6.0（图片加载，支持 `file://` 与 https）
- `sh.calvin.reorderable` 2.4.3（首页长按拖拽）
- Compile/Target SDK 34，Min SDK 24

## 工程结构

```
life-study/
├── settings.gradle.kts
├── build.gradle.kts                 # 项目级（AGP / Kotlin 版本）
├── gradle.properties
├── gradle/wrapper/                  # 已内置 wrapper，gradle 版本 8.7
└── app/
    ├── build.gradle.kts             # 应用级（依赖、SDK 版本）
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml      # 仅声明 INTERNET 权限
        ├── java/com/lifestudy/app/
        │   ├── MainActivity.kt
        │   ├── data/
        │   │   ├── Models.kt        # Category / Item 数据类
        │   │   ├── DataSource.kt    # 10 个内置分类的全部数据
        │   │   ├── Repository.kt    # 单例仓库：状态 + SharedPreferences 持久化
        │   │   └── ImageStorage.kt  # 把用户选图拷到 filesDir/images/
        │   └── ui/
        │       ├── nav/AppNav.kt    # Navigation Compose 路由
        │       ├── theme/           # Color / Theme / Type
        │       ├── components/
        │       │   ├── ItemImage.kt # 图片渲染（四级 fallback，见下）
        │       │   └── AddDialogs.kt
        │       └── screens/
        │           ├── HomeScreen.kt
        │           ├── CategoryScreen.kt
        │           └── DetailScreen.kt
        └── res/
            ├── drawable/            # 内置的本地缩略图（按 itemId 命名）
            └── ...                  # mipmap / values / xml
```

## 数据存储

| 数据 | 位置 | 说明 |
| --- | --- | --- |
| 分类与条目（含用户自定义） | `SharedPreferences("life_study_v1")` 里的 `categories`（JSON 字符串） | 改动后立即写回 |
| "已认识"集合 | 同 SharedPreferences 的 `learned_ids` | `Set<String>` |
| 用户上传的图片 | `context.filesDir/images/img_<ts>_<uuid>.jpg` | 保存的是 `file://` URL，可直接喂给 Coil |

App 启动时 `AppRepository.init()` 会做一次"合并升级"：
1. 删除已下线的内置分类
2. 用 `DataSource` 的最新文案/标签刷新已存档的内置条目，但保留用户上传的封面 / 额外图
3. 追加新出现的内置分类

## 图片渲染优先级（`ItemImage.kt`）

```
coverOverride (用户上传的封面, file://)
    ↓ 未设置
res/drawable/<itemId>.jpg | .png    （按条目 id 同名查找）
    ↓ 没找到
imageUrl (https://...)               （Coil 异步加载）
    ↓ 全部失败
分类 accent 色块 + 分类 emoji 兜底
```

> **当前已知占位限制**：未打包本地图的条目，`imageUrl` 走的是 `picsum.photos/seed/...`。它只能保证同一 seed 始终返回同一张图，但**不会按关键词主题匹配**，会显示随机风景照。要让某分类显示真实图片，把对应 `<itemId>.jpg` 放进 `app/src/main/res/drawable/` 即可（无需改代码）。

## 打包 APK

环境要求：JDK 17、Android SDK Platform 34、build-tools 34。
Gradle wrapper 已经放在仓库里，无需本机预装 Gradle。

### debug 包（最快路径，可直接装机）

```bash
./gradlew assembleDebug
# 产物: app/build/outputs/apk/debug/app-debug.apk  (~23 MB)
```

Debug 包用 Android 默认 debug keystore 签名，可直接 `adb install` 或拷到手机安装。

### release 包

`app/build.gradle.kts` 当前 release 块**没有 `signingConfig`**，直接执行 `assembleRelease` 只会产出 `app-release-unsigned.apk`，无法安装。要打可分发的 release，请先：

1. 准备好 keystore，例：
   ```bash
   keytool -genkey -v -keystore release.keystore \
     -alias lifestudy -keyalg RSA -keysize 2048 -validity 36500
   ```
2. 在 `app/build.gradle.kts` 的 `android { ... }` 内添加：
   ```kotlin
   signingConfigs {
       create("release") {
           storeFile = file("../release.keystore")
           storePassword = System.getenv("KSTORE_PWD")
           keyAlias = "lifestudy"
           keyPassword = System.getenv("KEY_PWD")
       }
   }
   buildTypes {
       release {
           signingConfig = signingConfigs.getByName("release")
           // 其余保持不变
       }
   }
   ```
3. 然后：
   ```bash
   KSTORE_PWD=... KEY_PWD=... ./gradlew assembleRelease
   # 产物: app/build/outputs/apk/release/app-release.apk
   ```

### Android Studio

打开本目录 → 等待 Gradle Sync → 菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**。

## 二次开发指引

- **新增一个内置分类**：在 `DataSource.kt` 里仿照已有 `dogs / cats / ...` 写一个 `Category`，加进文件底部的 `categories` 列表即可，UI 自动出现新卡片。`accent` 用 `0xFFxxxxxxL` 颜色。
- **新增一个内置条目**：在对应 `Category` 的 `items = listOf(...)` 里追加 `Item(...)`；如果想用本地图，把图按 `Item.id`.jpg 放进 `res/drawable/`。
- **修改主题色**：`ui/theme/Color.kt`；单个分类的强调色在 `DataSource.kt` 各 `Category` 的 `accent` 字段。
- **App 内新增分类 / 条目**：用户在 UI 上添加的分类 id 形如 `u_cat_<ts>`，条目 id 形如 `u_item_<ts>`，由 `Repository.addCategory` / `addItem` 维护，不会和内置 id 冲突。
- **清除本地缓存**：`AppRepository.resetToDefaults()` 会把分类恢复成内置默认值（不清 learned 集合）。

## License

仓库内的内置数据仅供学习参考；如要分发请自行核对版权（尤其是图片）。
