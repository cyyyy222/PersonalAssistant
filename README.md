# PersonalAssistant · Android 前端演示

Kotlin + Jetpack Compose 原生 UI，基于已有项目直接实现。纯白背景、轻灰卡片、深色文字、绿色重点操作；邮件页面采用 iOS 信息风格的会话列表与气泡详情，页面只保留必要操作和状态；未接入后端、邮箱或在线 AI。

## 已实现

| 页面 | 可操作内容 |
| --- | --- |
| 登录 | 账号或手机号 + 密码登录，登录态本机保存，退出登录 |
| 首页 | 日期问候、快捷指令、邮件/饮食入口、实时概览 |
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

## 登录与后端

Android 端启动时先显示登录页，登录成功后进入原有主界面。后端为独立的 Spring Boot 项目，位于 `server/`；数据库为 MySQL 8。

### 测试账号

| 项目 | 值 |
| --- | --- |
| 用户名 | `caiyangyang` |
| 手机号 | `13800138000` |
| 密码 | `123456789` |

### 数据库

- 地址：`127.0.0.1:3306`
- 用户名：`root`
- 密码：`a1s2d3f4g5h6`
- 数据库：`personal_assistant`
- 表：`users`

表结构由 `server/src/main/resources/schema.sql` 自动创建；首次启动后端时，`DataSeeder` 会写入测试账号，密码使用 BCrypt 哈希存储。

### 启动后端

```powershell
Set-Location 'D:\project_place\PersonalAssistant\server'
.\gradlew.bat bootRun
```

或在 VS Code 任务中运行「后端: 启动服务」。如果提示 `Port 8080 was already in use`，说明已有后端进程在运行，先执行任务「后端: 释放 8080 端口」，或在终端执行：

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

健康检查：

```text
GET http://127.0.0.1:8080/api/health
```

### 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/login` | 账号/手机号 + 密码登录，返回 JWT |
| GET | `/api/auth/me` | 当前用户，需 `Authorization: Bearer <token>` |
| POST | `/api/auth/logout` | 退出登录，需登录态 |

除登录和健康检查外，`/api/**` 均经过 `AuthInterceptor` 校验 JWT。密码错误、未登录、过期 token 返回 401；参数校验失败返回 400。

### Android 连接地址

默认后端地址为 `http://10.0.2.2:8080`，适用于 Android 模拟器。真机通过局域网连接时，用本机 IP 重新构建：

```powershell
.\gradlew.bat :app:installDebug -PapiBaseUrl=http://192.168.1.100:8080
```

Android 17 对本地网络访问有独立运行时权限；应用会在首次登录前请求 `ACCESS_LOCAL_NETWORK`。允许后即可访问本机或局域网后端。

## 真实邮箱（QQ / 163）

第一版已接通真实邮箱，后端支持：

- QQ、Foxmail、163、126 邮箱授权码绑定
- IMAP 收件箱同步
- 首次同步和增量同步
- UIDVALIDITY 变化时自动重建
- MIME 解析、纯文本正文提取
- 邮件列表分页、详情、已读状态
- 搜索、星标、归档
- 附件元数据、按需下载、本地磁盘存储
- SMTP 发信、CC/BCC、回复头、附件、发送后追加到已发送
- 每 10 分钟定时同步和手动同步
- 后端多邮箱支持

Android 端目前只接入了绑定、真实列表、详情、已读、写邮件和发送；阶段 D 的星标、归档、附件下载和多邮箱切换 UI 还未接入。

### App 内操作

1. 登录 App。
2. 进入「邮件」。
3. 点击「绑定邮箱」。
4. 输入 QQ 邮箱地址和授权码。
5. 绑定成功后会自动同步 INBOX。
6. 列表右上角「写邮件」可以发送邮件。

QQ 邮箱需要先在网页版设置中开启 IMAP 服务并生成授权码。授权码不是 QQ 登录密码。

### 邮箱接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/mail/accounts` | 已绑定邮箱 |
| POST | `/api/mail/accounts` | 绑定并测试 IMAP |
| DELETE | `/api/mail/accounts/{id}` | 解绑并清理缓存 |
| POST | `/api/mail/accounts/{id}/sync` | 同步 INBOX |
| GET | `/api/mail/messages` | 分页列表，支持 keyword/starred/archived |
| GET | `/api/mail/messages/{id}` | 邮件详情 |
| POST | `/api/mail/messages/{id}/read` | 标记已读/未读 |
| POST | `/api/mail/messages/{id}/star` | 星标 |
| POST | `/api/mail/messages/{id}/archive` | 移动到邮箱归档文件夹 |
| GET | `/api/mail/messages/{id}/attachments` | 附件列表 |
| GET | `/api/mail/attachments/{id}/download` | 下载附件 |
| POST | `/api/mail/send` | SMTP 发信，支持 CC/BCC/附件/回复头 |
| POST | `/api/mail/sync-all` | 手动同步当前用户全部邮箱 |

### 安全与限制

- 授权码使用 AES-256-GCM 加密后存入 `mail_accounts.secret_cipher`。
- API 不返回授权码。
- 只允许 QQ、Foxmail、163、126 域名，IMAP 服务器由后端映射，客户端不能随意指定。
- 第一版只同步 INBOX；归档会把邮件移动到邮箱的归档文件夹。
- 附件默认按需下载，存放在 `server/data/attachments`，数据库只保存元信息和 `storage_key`。
- 附件存储已抽象为 `AttachmentStorage`：当前 `LocalAttachmentStorage`，`S3AttachmentStorage` 为后续对象存储预留的半成品。
- 发信使用同一授权码；发送成功后会尝试 IMAP APPEND 到已发送文件夹。
- 定时同步默认每 10 分钟，后端未运行时不会执行。
- 生产环境应把 `app.mail.secret-key`、数据库密码和 JWT 密钥迁移到环境变量。

## 在 VS Code 中查看界面（不带手机）

已安装 VS Code 扩展 **EmbeDroid**，它把 Android 模拟器画面嵌入 VS Code 编辑器标签页，可直接用鼠标点击、滑动和输入。

1. 点击左侧活动栏的 **Android 机器人图标**（Android Emulator）。
2. 在 **Running Devices** 中点击正在运行的模拟器，画面会在编辑器标签页中打开。
3. 若没有运行中的设备，在 **Available AVDs** 中点击 `Medium_Phone_API_37.0`，扩展会以无窗口模式启动模拟器。
4. 若图标未出现，执行 `Ctrl+Shift+P` → **Developer: Reload Window**。

EmbeDroid 1.0.2 自带的 scrcpy 3.2 在 Android 17 上会因 `IDisplayWindowListener` 接口变化崩溃。项目已下载 scrcpy 5.0.1 的 server 并在 VS Code 设置中指向：

```text
D:\project_place\PersonalAssistant\tools\scrcpy-server.jar
```

修改设置后需要重载 VS Code 窗口。如果内嵌画面仍无法建立，可使用任务「镜像模拟器 (scrcpy)」打开独立镜像窗口；`tools/` 仅本机使用，不纳入 Git。

日常改完代码后，在 VS Code 中按 `Ctrl+Shift+B` 运行默认任务「Android: 构建、安装并启动」，即可重新安装到模拟器并打开应用。也可以在终端手动执行：

```powershell
$env:JAVA_HOME = 'D:\android studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;D:\Android\Sdk\platform-tools;$env:Path"
adb devices
.\gradlew.bat :app:installDebug
adb shell am start -n com.example.personalassistant/.MainActivity
```

`gradle.properties` 已固定使用 Android Studio 完整 JDK，避免 VS Code Java 扩展的精简 JRE 缺少 `jlink.exe` 导致构建失败。项目内的 `.vscode/tasks.json` 提供构建安装和重新启动两个任务。

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

新增逻辑测试覆盖搜索与筛选组合、24 小时时间校验、重新生成模板；设备测试覆盖登录表单校验、邮件编辑/确认/模拟发送、饮食必填校验/保存/编辑、表单状态恢复、搜索空状态与归档。

AGP、Gradle wrapper、SDK、Kotlin、Compose BOM 与应用运行依赖均沿用现有配置。仅测试依赖更新 `espresso-core 3.5.1 → 3.7.0`、`ext:junit 1.1.5 → 1.3.0`，用于兼容已连接的 Android 17 模拟器。原版 Espresso 在该设备上通过反射调用已移除的 `InputManager.getInstance` 导致测试启动失败；3.7.0 改用系统服务获取。[AndroidX Test 官方发布说明](https://developer.android.com/jetpack/androidx/releases/test)。

lint 可能报告现有旧版依赖的升级提示及 `lifecycle-runtime 2.6.1` 的自定义 lint API 不兼容提示；为保留现有运行依赖，本次不额外升级。

## 代码位置与适配

- `data/DemoData.kt`：数据模型、示例内容、筛选与时间校验。
- `data/Auth.kt`：Android 登录请求、登录会话与用户模型。
- `data/AssistantState.kt`：本地模拟动作、登录态、持久化、页面与表单状态。
- `ui/LoginScreen.kt`：账号/手机号登录界面与本地网络权限提示。
- `ui/AssistantApp.kt`：登录入口、主框架、返回、底部导航与操作区。
- `ui/HomeScreen.kt`、`MailScreens.kt`、`FoodScreens.kt`：八个页面；邮件页为 iOS 信息式会话列表与气泡阅读。
- `ui/Components.kt`：统一线性图标、分段控件、卡片、输入框与按钮。
- `ui/theme/`：色彩、字体、圆角。
- `server/`：Spring Boot + MySQL 后端，包含登录、JWT、拦截器、统一响应与异常处理。
- [真实邮箱接入规划](docs/email-integration-plan.md)：IMAP 代理、表结构、接口、同步流程与安全设计。

不固定物理像素或屏幕高度，使用 dp/sp、自适应宽度与可滚动内容；页面最大内容宽度 640dp。两列操作在窄屏或大字体时改为纵向排列；筛选和餐次横向可滚动；处理状态栏、挖孔、手势区和软键盘。主题始终保持浅色，不跟随系统深色模式或壁纸动态色。Home Preview 提供 412×915dp 和 360×800dp/1.5 倍字体视口。本次 vivo V2309A 实测为 1260×2800 像素、560dpi，对应约 360×800dp；实际显示以系统设置为准。窗口处理参考 [Compose 官方 Insets 指南](https://developer.android.com/develop/ui/compose/system/insets-ui)；JDK 参数参考 [Gradle Toolchains](https://docs.gradle.org/current/userguide/toolchains.html)。

## 本次验证结果（2026-11-11）

- Debug APK 构建成功，单元测试 4/4 通过。
- Android 17 模拟器设备测试 6/6 通过，包含登录表单校验。
- 后端构建成功；`/api/health`、账号登录、手机号登录、`/api/auth/me` 与未登录 401 拦截均已验证，MySQL `personal_assistant.users` 已写入测试账号。
- 已新增邮箱表 `mail_accounts`、`mail_sync_state`、`mail_messages`、`mail_attachments`；邮箱绑定、解绑、同步、分页、详情、已读和发信接口均已编译通过。无效 QQ 授权码返回 401，未支持域名返回 400。
- 阶段 D 后端已编译通过：搜索过滤、星标、真实归档移动、附件元数据/下载、本地存储抽象、S3 存储占位实现、CC/BCC/回复头/附件发信、追加已发送、10 分钟定时同步、多邮箱同步接口。真实 QQ 的归档文件夹、星标回写和附件下载仍需要你的授权码做一次端到端验证。
- Android 17 模拟器上使用 `caiyangyang / 123456789` 登录成功并进入首页；登录态已持久化。
- [八页界面总览](docs/ui-overview.png)，逐页原图在 `docs/screenshots/`。
