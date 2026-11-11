# 真实邮箱接入规划

> 当前进度：阶段 A、B、C 第一版已实现（QQ / Foxmail / 163 / 126）；阶段 D 的后端已实现搜索、星标、归档、附件元数据与下载、SMTP CC/BCC/回复头/附件、追加已发送、10 分钟定时同步和多邮箱支持。AI 摘要暂未接入。Android 端暂未接入阶段 D 的星标、归档、附件下载和多邮箱切换 UI。

## 1. 目标

把当前本地示例邮件替换为真实邮箱：

- 用户在后端绑定自己的邮箱账号。
- 后端通过 IMAP 拉取真实邮件，Android 端通过现有 JWT 接口读取。
- 先做只读收件箱，稳定后再做已读/星标/归档，最后考虑发信。
- 不把邮箱密码或授权码下发到 Android，不在日志中输出敏感信息。

## 2. 推荐方案：后端 IMAP 代理

```
Android App
   │  JWT + HTTPS/HTTP(dev)
   ▼
Spring Boot 后端
   │  IMAP over TLS (993)
   ▼
QQ / 163 / Gmail / Outlook 邮箱
```

选择 IMAP 而不是让 Android 直连邮箱的原因：

1. 邮箱授权码只保存在服务端，可加密存储。
2. 不同邮箱协议、编码、附件解析统一由后端处理。
3. Android 端只面对稳定的 JSON 接口，后续接发信、搜索、AI 摘要都容易扩展。
4. 可以增量同步、缓存、分页，避免手机反复拉全量邮件。

第一阶段建议先支持 **QQ 邮箱或 163 邮箱**，使用 IMAP 授权码。

## 3. 数据库设计

新增表：

### `mail_accounts`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| user_id | BIGINT | 关联 users.id |
| email | VARCHAR(255) | 邮箱地址 |
| display_name | VARCHAR(128) | 显示名 |
| imap_host | VARCHAR(128) | 如 imap.qq.com |
| imap_port | INT | 993 |
| imap_ssl | TINYINT | 1 |
| username | VARCHAR(255) | IMAP 用户名 |
| secret_cipher | TEXT | AES-GCM 加密后的授权码 |
| status | TINYINT | 正常/失效 |
| last_sync_at | DATETIME | 最近同步时间 |
| created_at / updated_at | DATETIME | 时间 |

### `mail_sync_state`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| account_id | BIGINT | 邮箱账号 |
| folder | VARCHAR(64) | INBOX 等 |
| uid_validity | BIGINT | IMAP UIDVALIDITY |
| last_uid | BIGINT | 已同步的最大 UID |
| last_sync_at | DATETIME | 最近同步时间 |

### `mail_messages`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT PK | 主键 |
| account_id | BIGINT | 邮箱账号 |
| folder | VARCHAR(64) | 文件夹 |
| uid | BIGINT | IMAP UID |
| message_id | VARCHAR(255) | Message-ID |
| subject | VARCHAR(512) | 主题 |
| from_name | VARCHAR(255) | 发件人 |
| from_address | VARCHAR(255) | 发件地址 |
| to_addresses | TEXT | 收件人 |
| sent_at | DATETIME | 发送时间 |
| snippet | VARCHAR(512) | 摘要 |
| body_text | MEDIUMTEXT | 纯文本正文 |
| body_html | MEDIUMTEXT | HTML 正文，可选 |
| has_attachments | TINYINT | 是否有附件 |
| is_read | TINYINT | 已读 |
| is_starred | TINYINT | 星标 |
| created_at | DATETIME | 入库时间 |

唯一索引：`(account_id, folder, uid)`。

## 4. 后端接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/mail/accounts` | 当前用户绑定的邮箱 |
| POST | `/api/mail/accounts` | 绑定邮箱并测试连接 |
| DELETE | `/api/mail/accounts/{id}` | 解绑 |
| POST | `/api/mail/accounts/{id}/sync` | 手动同步 |
| GET | `/api/mail/messages` | 分页列表，支持 folder/keyword/未读 |
| GET | `/api/mail/messages/{id}` | 邮件详情 |
| POST | `/api/mail/messages/{id}/read` | 标记已读 |
| POST | `/api/mail/messages/{id}/star` | 星标 |
| POST | `/api/mail/send` | 第二阶段：SMTP 发信 |

统一沿用当前 `ApiResponse`、JWT 和 `AuthInterceptor`。

## 5. 同步流程

1. 用户绑定邮箱：后端用 IMAP `LOGIN` 测试连接，成功后加密保存授权码。
2. 首次同步：`SELECT INBOX`，读取 `UIDVALIDITY`，按 UID 分批（如 50 封）拉取。
3. 解析邮件：主题、发件人、时间、纯文本/HTML 正文、附件标记。
4. 写入 `mail_messages`，更新 `mail_sync_state.last_uid`。
5. 增量同步：只拉取 `UID > last_uid` 的邮件。
6. `UIDVALIDITY` 改变时清空该文件夹缓存并全量重建。
7. 同步失败记录状态与错误，不阻塞列表读取；已缓存邮件仍可查看。

建议第一版同步策略：

- 打开邮件页时触发一次增量同步。
- 手动下拉刷新。
- 暂不做后台定时同步，避免桌面开发环境长期占用连接。

## 6. 安全设计

- 授权码使用 AES-256-GCM 加密，密钥放在环境变量，不写进 Git。
- API 不返回授权码，只返回邮箱地址和连接状态。
- 日志过滤密码、授权码、完整正文。
- 默认只读，不自动发送、不自动删除邮件。
- 绑定接口做频率限制，避免暴力尝试。
- 生产环境使用 HTTPS；开发环境才允许 HTTP。
- 邮箱密码/授权码由用户在 App 内输入，不通过聊天或配置文件明文传递。

## 7. Android 端改造

1. 新增「邮箱设置」页面：邮箱地址、IMAP 授权码、服务器自动识别。
2. 邮件列表从 `DemoData` 切换为后端分页数据。
3. 保留当前 iOS 信息风格列表与气泡详情。
4. 增加加载中、空状态、同步失败重试。
5. 详情页按需加载完整正文。
6. 已读/星标先在本地乐观更新，再同步后端。
7. 令牌过期时自动回到登录页。

## 8. 邮箱服务商差异

| 邮箱 | IMAP | 说明 |
| --- | --- | --- |
| QQ 邮箱 | `imap.qq.com:993` | 需在设置中开启 IMAP 并生成授权码 |
| 163 邮箱 | `imap.163.com:993` | 需开启 IMAP 并使用授权码 |
| Gmail | `imap.gmail.com:993` | 需开启两步验证并使用应用专用密码 |
| Outlook / Microsoft 365 | `outlook.office365.com:993` | 基本认证已关闭，建议走 OAuth2 |

第一版优先 QQ/163 授权码方式，OAuth2 放在后续阶段。

## 9. 开发阶段

### 阶段 A：后端邮箱模型与绑定

- 建表、加密工具、绑定/解绑接口。
- 用 JavaMail（`angus-mail`）测试 IMAP 连接。
- 完成连接错误码与提示。

### 阶段 B：收件箱同步

- 首次同步、增量同步、UIDVALIDITY 处理。
- 邮件 MIME 解析、正文提取、分页接口。
- 用真实邮箱验证 QQ/163。

### 阶段 C：Android 接入

- 邮箱绑定页、真实列表、详情、刷新。
- 替换本地示例数据，保留离线缓存。
- 已读状态同步。

### 阶段 D：增强

- 搜索、附件、星标、归档。
- SMTP 发信、回复、AI 摘要。
- 定时同步与多邮箱。

## 10. 开始开发前需要确认

1. 使用哪个邮箱服务商：QQ、163、Gmail 或 Outlook。
2. 第一阶段是否只读收件箱，还是同时需要发信。
3. 邮箱授权码由用户在 App 内输入，还是先在后端本地配置一个测试账号。
4. 是否需要保留当前本地示例邮件作为「演示模式」。

建议先按「QQ/163 + 只读收件箱 + App 内输入授权码」实现，范围最小、最快看到真实邮件。
