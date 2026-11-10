# PersonalAssistant · Android 前端演示

Kotlin + Jetpack Compose 原生 UI，基于已有项目直接实现。纯白背景、轻灰卡片、深色文字、绿色重点操作；邮件页面采用 iOS 信息风格的会话列表与气泡详情，页面只保留必要操作和状态；未接入后端、邮箱或在线 AI。

## 已实现

| 页面 | 可操作内容 |
| --- | --- |
| 首页 | 日期问候、快捷指令、邮件/饮食入口、实时概览、最近活动 |
| 邮件列表 | iOS 信息风格会话列表：头像、未读圆点、单行摘要；搜索发件人/邮箱/主题/正文，全部/未读/待回复/已归档筛选，空状态 |
| 邮件详情 | 气泡阅读、打开即标为已读、本地 AI 总结、顶栏待回复标记、归档/移回收件箱 |
| AI 回复草稿 | 本地模板、正文编辑、确认后重新生成、保存草稿 |
| 发送确认 | 完整预览、返回修改、模拟发送、完成反馈 |
| 饮食首页 | 今日餐次进度、分餐次快捷记录、今日列表、点击编辑 |
| 新增/编辑饮食 | 餐次、必填食物、可选备注、原生时间选择、校验、保存 |

底栏仅出现在首页/邮件/饮食三个主页面；次级页面使用顶部返回和 Android 系统返回。主页面返回时保留搜索、筛选和滚动位置。从首页进入饮食表单，返回后仍回首页。

**不能真实发邮件。** 所有邮箱均为 `example.com` 示例地址。总结、回复、重新生成使用本地预置文本；点击“模拟发送”只修改本地状态，不会发邮件或联系收件人。首页指令仅支持包含“邮件/邮箱/饮食/早餐/午餐/晚餐/加餐/吃”的关键词路由，其他输入会提示支持的操作。

饮食、草稿、邮件的已读/归档/待回复/模拟回复状态使用本地 SharedPreferences 保存；表单、编辑中的正文与导航使用 `rememberSaveable` 恢复。初次运行包含 5 封示例邮件、当天 2 条示例饮食；后续按记录日期显示“今天”。返回草稿详情时自动保存草稿；退出未保存的饮食表单会放弃本次编辑。卸载/清除数据可重置演示。

## 在 VS Code 的 PowerShell 终端运行

1. 在 VS Code 打开 `D:\project_place\PersonalAssistant`。
2. vivo X100 开启“开发者选项 → USB 调试”，USB 连接电脑并接受手机上的调试授权。如系统提示安装权限，允许通过 USB 安装。
3. 使用已有 Android Studio 自带的完整 JDK（当前项目的 daemon JVM 条件为 Java 25）。本机 SDK 已配置在 `local.properties`：`D:\Android\Sdk`，无需修改 Gradle 版本。

```powershell
Set-Location 'D:\project_place\PersonalAssistant'
$env:JAVA_HOME = 'D:\android studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;D:\Android\Sdk\platform-tools;$env:Path"
java -version
adb devices -l
.\gradlew.bat installDebug
```

`adb devices -l` 中真机状态应为 `device`；`unauthorized` 需要在手机上接受授权。安装后点击手机上的 PersonalAssistant 图标，或执行：

```powershell
adb -d shell am start -n com.example.personalassistant/.MainActivity
```

同时连接多个设备时，推荐明确指定真机（本次检测序列号为 `10AE891M5X008S6`，以后以 `adb devices -l` 为准）：

```powershell
$env:ANDROID_SERIAL = '10AE891M5X008S6'
.\gradlew.bat installDebug '-Pandroid.injected.device.serial=10AE891M5X008S6'
adb -s 10AE891M5X008S6 shell am start -n com.example.personalassistant/.MainActivity
```

如果 Gradle 自动选中了 VS Code Java 扩展的精简 JRE，出现 `jlink.exe does not exist`，用本机完整 JDK 临时覆盖自动检测，不必更改项目配置：

```powershell
.\gradlew.bat installDebug '-Dorg.gradle.java.installations.auto-detect=false' '-Dorg.gradle.java.installations.paths=D:\android studio\jbr' '-Pandroid.injected.device.serial=10AE891M5X008S6'
```

若需要调试安装失败信息，加 `--stacktrace`。APK 输出：`app\build\outputs\apk\debug\app-debug.apk`。`INSTALL_FAILED_UPDATE_INCOMPATIBLE` 表示旧包签名不同，请先备份需要的本地数据，再手动卸载旧版本后安装。

若出现 `INSTALL_FAILED_ABORTED: User rejected permissions`，请解锁 vivo 并在手机的安装确认窗口允许本次安装，再运行安装命令；此问题与代码编译无关。真机检查时建议保持屏幕亮起。

## 检查与测试

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
# 指定已连接模拟器，运行 Compose 流程测试和七页截图
$env:ANDROID_SERIAL = 'emulator-5554'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.injected.device.serial=emulator-5554' '-Dorg.gradle.java.installations.auto-detect=false' '-Dorg.gradle.java.installations.paths=D:\android studio\jbr'
```

设备测试通常会在结束时卸载测试安装并清理截图。需要导出七页截图时，手动运行截图用例后立即导出：

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -e class com.example.personalassistant.AssistantFlowTest#captureSevenScreensForVisualReview com.example.personalassistant.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 pull /sdcard/Android/data/com.example.personalassistant/files/screenshots docs/
```

新增逻辑测试覆盖搜索与筛选组合、24 小时时间校验、重新生成模板；设备测试覆盖邮件编辑/确认/模拟发送、饮食必填校验/保存/编辑、表单状态恢复、搜索空状态与归档。

AGP、Gradle wrapper、SDK、Kotlin、Compose BOM 与应用运行依赖均沿用现有配置。仅测试依赖更新 `espresso-core 3.5.1 → 3.7.0`、`ext:junit 1.1.5 → 1.3.0`，用于兼容已连接的 Android 17 模拟器。原版 Espresso 在该设备上通过反射调用已移除的 `InputManager.getInstance` 导致测试启动失败；3.7.0 改用系统服务获取。[AndroidX Test 官方发布说明](https://developer.android.com/jetpack/androidx/releases/test)。

lint 可能报告现有旧版依赖的升级提示及 `lifecycle-runtime 2.6.1` 的自定义 lint API 不兼容提示；为保留现有运行依赖，本次不额外升级。

## 代码位置与适配

- `data/DemoData.kt`：数据模型、示例内容、筛选与时间校验。
- `data/AssistantState.kt`：本地模拟动作、持久化、页面与表单状态。
- `ui/AssistantApp.kt`：主框架、返回、底部导航与操作区。
- `ui/HomeScreen.kt`、`MailScreens.kt`、`FoodScreens.kt`：七个页面；邮件页为 iOS 信息式会话列表与气泡阅读。
- `ui/Components.kt`：统一线性图标、卡片、输入框、按钮与 AI 提示。
- `ui/theme/`：色彩、字体、圆角。

不固定物理像素或屏幕高度，使用 dp/sp、自适应宽度与可滚动内容；页面最大内容宽度 640dp。两列操作在窄屏或大字体时改为纵向排列；筛选和餐次横向可滚动；处理状态栏、挖孔、手势区和软键盘。主题始终保持浅色，不跟随系统深色模式或壁纸动态色。Home Preview 提供 412×915dp 和 360×800dp/1.5 倍字体视口。本次 vivo V2309A 实测为 1260×2800 像素、560dpi，对应约 360×800dp；实际显示以系统设置为准。窗口处理参考 [Compose 官方 Insets 指南](https://developer.android.com/develop/ui/compose/system/insets-ui)；JDK 参数参考 [Gradle Toolchains](https://docs.gradle.org/current/userguide/toolchains.html)。

## 本次验证结果（2026-11-10）

- Debug APK 构建成功，单元测试 4/4 通过（其中 3 项为本次新增）。
- Android 17 模拟器设备测试 6/6 通过（其中 5 项为本次新增）。最终 APK 重新执行设备测试亦全部通过。
- lint：0 errors、14 warnings，均为模板原有资源、标签或旧依赖提示；旧生命周期依赖的部分自定义 lint 检查因 API 兼容问题被跳过。
- 已在 vivo X100 / V2309A（Android 16）成功安装并启动；验证首页显示、邮件详情、系统返回、饮食页切换、饮食表单及软键盘弹出。首页纯白背景、状态栏与底部手势区正常显示。
- [七页界面总览](docs/ui-overview.png)，逐页原图在 `docs/screenshots/`。
