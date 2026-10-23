# SimpleLauncher

一个使用 Kotlin + AndroidX 实现的轻量级 Android **桌面（Launcher）** 应用：干净、可读、可直接安装到手机或模拟器上替换系统桌面。

## 功能

| 分类 | 说明 |
| --- | --- |
| 桌面主页 | `HOME` 类别，可被系统设置为默认 Launcher；显示系统壁纸、时钟日期 |
| 分页桌面 | ViewPager2 分页网格，横向滑动切页，底部圆点指示器 |
| 全部应用抽屉 | 上滑或点击网格图标打开，支持关键词搜索（应用名 / 包名）、下滑或返回键关闭 |
| Dock 栏 | 底部常驻应用，长按图标可添加 / 移除，使用 SharedPreferences 持久化 |
| 应用图标菜单 | 长按弹出：应用信息、卸载、加入 / 移出 Dock |
| 实时更新 | 监听 `PACKAGE_ADDED / REMOVED / REPLACED / CHANGED`，安装或卸载应用后自动刷新 |
| 图标缓存 | `LruCache` 缓存已解码图标，滑动不卡顿 |
| 壁纸入口 | 桌面空白处长按可拉起系统壁纸选择器 |

## 手势

- 桌面上滑 → 打开全部应用
- 桌面空白处长按 → 更换壁纸
- 抽屉中下滑 → 返回桌面

## 技术栈

- Kotlin 1.9.25 + Coroutines
- Android Gradle Plugin 8.5.2 / Gradle 8.9
- androidx: appcompat 1.7.0、core-ktx 1.13.1、recyclerview 1.3.2、viewpager2 1.1.0
- ViewBinding，不使用任何第三方 UI 库
- `minSdk 26` / `targetSdk 34`

## 本地构建

```bash
git clone https://github.com/Tsu2014/SimpleLauncher.git
cd SimpleLauncher
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

或用 Android Studio 直接打开工程根目录。

## 安装与使用

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity com.tsu.launcher/.ui.home.HomeActivity
```

安装后按 Home 键，在系统弹窗中选择 **SimpleLauncher** 并设为「始终」。

> 在模拟器 / 开发机上调试：Settings → Apps → Default apps → Home app。

## 目录结构

```
app/src/main/java/com/tsu/launcher/
├── data/
│   ├── AppRepository.kt        # 读取已安装应用、变更通知
│   ├── LauncherPreferences.kt  # Dock 配置持久化
│   └── PackageChangedReceiver.kt
├── model/AppInfo.kt
├── ui/
│   ├── common/                 # 通用适配器 / 手势容器 / 间距装饰
│   ├── home/                   # 桌面主页 + 分页
│   └── apps/AllAppsActivity.kt # 全部应用抽屉
└── util/                       # 图标缓存、dp/列数计算、启动辅助
```

## 后续可扩展

- AppWidget 支持（`AppWidgetHost`）
- 图标拖拽、文件夹、桌面重命名
- 隐藏应用、应用抽屉按字母分组索引
- 桌面手势自定义（双击锁屏、下拉通知栏）

## License

MIT
