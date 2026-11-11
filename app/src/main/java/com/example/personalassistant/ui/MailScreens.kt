package com.example.personalassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalassistant.data.*
import com.example.personalassistant.ui.theme.*

private val mailFilters = listOf("全部", "未读", "待回复", "已归档")

@Composable
fun MailListScreen(
    state: AssistantState,
    onBind: () -> Unit,
    onCompose: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRemote: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onUnreadChange: (Boolean) -> Unit
) {
    when {
        state.mailAccount != null -> RemoteMailListScreen(
            state, onBind, onCompose, onRefresh, onOpenRemote, onQueryChange, onUnreadChange
        )
        state.demoMailMode -> DemoMailListScreen(state)
        else -> BindMailPrompt(onBind)
    }
}

@Composable
private fun DemoMailListScreen(state: AssistantState) {
    val results = state.mails.filter { matchesMail(it, state.query, state.filter) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("邮件", style = MaterialTheme.typography.headlineLarge)
                MailSearchField(state.query) { state.query = it }
                MailFilterBar(state.filter) { state.filter = it }
            }
        }
        item { HorizontalDivider(Modifier.padding(top = 12.dp), thickness = 0.5.dp, color = Line) }
        if (results.isEmpty()) item {
            EmptyState("没有找到邮件", action = "查看全部", onClick = {
                state.query = ""
                state.filter = "全部"
            }, glyph = Glyph.Search)
        }
        items(results, key = { it.id }) { mail -> MailRow(mail) { state.openMail(mail.id) } }
    }
}

@Composable
private fun BindMailPrompt(onBind: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("邮件", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(20.dp))
        EmptyState("未绑定邮箱", "绑定 QQ 邮箱后即可读取真实收件箱",
            action = "绑定邮箱", onClick = onBind, glyph = Glyph.Mail)
    }
}

@Composable
private fun RemoteMailListScreen(
    state: AssistantState,
    onBind: () -> Unit,
    onCompose: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRemote: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onUnreadChange: (Boolean) -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("邮件", Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                    TextButton(onClick = onCompose) { Text("写邮件", style = MaterialTheme.typography.labelMedium) }
                    TextButton(onClick = onBind) { Text("邮箱", style = MaterialTheme.typography.labelMedium) }
                }
                MailSearchField(state.mailQuery, onQueryChange)
                SegmentedControl(
                    listOf("全部", "未读"),
                    if (state.mailUnreadOnly) "未读" else "全部",
                    onSelect = { onUnreadChange(it == "未读") }
                )
                if (state.mailSyncing) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green,
                    trackColor = Line)
                state.mailError?.let { error ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(error, Modifier.weight(1f), color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRefresh) { Text("重试") }
                    }
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(top = 12.dp), thickness = 0.5.dp, color = Line) }
        if (!state.mailLoading && !state.mailSyncing && state.remoteMails.isEmpty()) item {
            EmptyState("收件箱是空的", action = "刷新", onClick = onRefresh, glyph = Glyph.Mail)
        }
        items(state.remoteMails, key = { it.id }) { mail ->
            RemoteMailRow(mail) { onOpenRemote(mail.id) }
        }
    }
}

@Composable
private fun RemoteMailRow(mail: RemoteMail, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("remote_mail_${mail.id}")) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(14.dp), contentAlignment = Alignment.Center) {
                if (!mail.read) Box(Modifier.size(8.dp).background(Green, CircleShape))
            }
            MailAvatar(mail.fromName.ifBlank { mail.fromAddress }, !mail.read)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mail.fromName.ifBlank { mail.fromAddress }, Modifier.weight(1f), maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp,
                            fontWeight = if (mail.read) FontWeight.Medium else FontWeight.SemiBold))
                    Spacer(Modifier.width(8.dp))
                    Text(formatMailTime(mail.sentAt), style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Spacer(Modifier.height(2.dp))
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = Ink, fontWeight = if (mail.read) FontWeight.Medium else FontWeight.SemiBold)) {
                        append(mail.subject)
                    }
                    if (mail.snippet.isNotBlank()) {
                        append("  ")
                        withStyle(SpanStyle(color = Muted, fontWeight = FontWeight.Normal)) { append(mail.snippet) }
                    }
                }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(Modifier.padding(start = 94.dp), thickness = 0.5.dp, color = Line)
    }
}

private fun formatMailTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        val time = java.time.LocalDateTime.parse(value)
        if (time.toLocalDate() == java.time.LocalDate.now()) {
            time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            time.format(java.time.format.DateTimeFormatter.ofPattern("M月d日"))
        }
    }.getOrDefault("")
}

@Composable
private fun MailSearchField(query: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 38.dp).background(SoftGray, RoundedCornerShape(10.dp))
        .padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIcon(Glyph.Search, Modifier.size(17.dp), tint = Muted)
        Spacer(Modifier.width(7.dp))
        BasicTextField(query, onChange, Modifier.weight(1f), singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
            cursorBrush = SolidColor(Green),
            decorationBox = { field ->
                if (query.isEmpty()) Text("搜索", style = MaterialTheme.typography.bodyMedium, color = Muted)
                field()
            })
        if (query.isNotEmpty()) Box(Modifier.size(20.dp).clickable { onChange("") }, contentAlignment = Alignment.Center) {
            AppIcon(Glyph.Close, Modifier.size(14.dp), tint = Muted, description = "清空搜索")
        }
    }
}

@Composable
private fun MailFilterBar(selected: String, onSelect: (String) -> Unit) {
    SegmentedControl(mailFilters, selected, onSelect)
}

@Composable
private fun MailRow(mail: Mail, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("mail_${mail.id}")) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(14.dp), contentAlignment = Alignment.Center) {
                if (mail.unread) Box(Modifier.size(8.dp).background(Green, CircleShape))
            }
            MailAvatar(mail.sender, mail.unread)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mail.sender, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp,
                            fontWeight = if (mail.unread) FontWeight.SemiBold else FontWeight.Medium))
                    Spacer(Modifier.width(8.dp))
                    Text(mail.time, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Spacer(Modifier.height(2.dp))
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(color = Ink, fontWeight = if (mail.unread) FontWeight.SemiBold else FontWeight.Medium)) {
                        append(mail.subject)
                    }
                    append("  ")
                    withStyle(SpanStyle(color = Muted, fontWeight = FontWeight.Normal)) { append(mail.preview) }
                }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
            }
        }
        HorizontalDivider(Modifier.padding(start = 94.dp), thickness = 0.5.dp, color = Line)
    }
}

@Composable
private fun MailAvatar(name: String, unread: Boolean = false, size: Int = 50) {
    Surface(Modifier.size(size.dp), CircleShape, color = if (unread) SoftGreen else SoftGray,
        contentColor = if (unread) Green else Muted) {
        Box(contentAlignment = Alignment.Center) {
            Text(name.take(1), fontWeight = FontWeight.SemiBold, fontSize = (size * 0.4f).sp)
        }
    }
}

@Composable
fun MailDetailScreen(state: AssistantState) {
    val mail = state.selectedMail
    PageColumn(spacing = 18.dp) {
        Text(mail.subject, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MailAvatar(mail.sender, size = 32)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(mail.sender, style = MaterialTheme.typography.titleSmall)
                Text(mail.time, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            val status = when {
                mail.archived -> "已归档"
                mail.simulatedSent -> "已回复"
                mail.waiting -> "待回复"
                else -> null
            }
            if (status != null) Text(status, color = if (mail.waiting && !mail.archived) Green else Muted,
                style = MaterialTheme.typography.labelMedium)
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp,
            bottomEnd = 20.dp, bottomStart = 6.dp), color = SoftGray, contentColor = Ink) {
            Text(mail.body, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
        if (state.showSummary) Surface(Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
            color = SoftGreen, contentColor = Ink) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("AI 总结", color = Green, style = MaterialTheme.typography.titleSmall)
                Text(mail.summary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun DraftScreen(state: AssistantState, onRegenerate: () -> Unit, onSaved: () -> Unit) {
    PageColumn(spacing = 18.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(state.selectedMail.sender, style = MaterialTheme.typography.titleMedium)
            Text("回复：${state.selectedMail.subject}", style = MaterialTheme.typography.bodySmall,
                color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        EntryField(state.draft, { state.draft = it }, "回复正文", "输入回复…", minLines = 12)
        AdaptivePair(
            first = { SecondaryButton("重新生成", onRegenerate, it, Glyph.Refresh) },
            second = { SecondaryButton("保存草稿", onSaved, it, Glyph.Save) }
        )
    }
}

@Composable
fun MailAccountScreen(state: AssistantState, onUnbind: () -> Unit) {
    val account = state.mailAccount
    PageColumn(spacing = 18.dp) {
        if (account != null) {
            Text("已绑定", color = Muted, style = MaterialTheme.typography.bodySmall)
            Text(account.email, style = MaterialTheme.typography.titleMedium)
            if (account.displayName.isNotBlank()) {
                Text(account.displayName, color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onUnbind) {
                Text("解绑邮箱", color = MaterialTheme.colorScheme.error)
            }
        } else {
            var showSecret by rememberSaveable { mutableStateOf(false) }
            val fieldColors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SoftGray, focusedContainerColor = SoftGray,
                unfocusedBorderColor = Color.Transparent, focusedBorderColor = Green,
                cursorColor = Green
            )
            OutlinedTextField(
                value = state.mailBindEmail,
                onValueChange = { state.mailBindEmail = it; state.mailBindError = null },
                modifier = Modifier.fillMaxWidth().testTag("mail_account_email"),
                singleLine = true,
                placeholder = { Text("QQ 邮箱地址") },
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = fieldColors
            )
            OutlinedTextField(
                value = state.mailBindSecret,
                onValueChange = { state.mailBindSecret = it; state.mailBindError = null },
                modifier = Modifier.fillMaxWidth().testTag("mail_account_secret"),
                singleLine = true,
                placeholder = { Text("邮箱授权码") },
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = fieldColors,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        AppIcon(if (showSecret) Glyph.EyeOff else Glyph.Eye, Modifier.size(20.dp),
                            tint = Muted, description = if (showSecret) "隐藏授权码" else "显示授权码")
                    }
                }
            )
            OutlinedTextField(
                value = state.mailBindDisplayName,
                onValueChange = { state.mailBindDisplayName = it },
                modifier = Modifier.fillMaxWidth().testTag("mail_account_name"),
                singleLine = true,
                placeholder = { Text("显示名称（可选）") },
                shape = RoundedCornerShape(16.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = fieldColors
            )
            Text("QQ 邮箱需开启 IMAP 并使用授权码，不是登录密码。",
                color = Muted, style = MaterialTheme.typography.bodySmall)
            state.mailBindError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun RemoteMailDetailScreen(state: AssistantState) {
    PageColumn(spacing = 18.dp) {
        when {
            state.remoteDetailLoading -> Box(Modifier.fillMaxWidth().padding(top = 80.dp),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
            state.remoteDetailError != null -> Text(state.remoteDetailError!!,
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            state.remoteDetail != null -> {
                val detail = state.remoteDetail!!
                Text(detail.subject, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold))
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MailAvatar(detail.fromName.ifBlank { detail.fromAddress }, size = 32)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(detail.fromName.ifBlank { detail.fromAddress }, style = MaterialTheme.typography.titleSmall)
                        Text(formatDetailTime(detail.sentAt), style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
                Surface(Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
                    color = SoftGray, contentColor = Ink) {
                    SelectionContainer {
                        Text(detail.bodyText.ifBlank { "无正文" }, Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (detail.hasAttachments) {
                    Text("${detail.attachmentCount} 个附件", color = Muted,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ComposeMailScreen(state: AssistantState) {
    PageColumn(spacing = 16.dp) {
        EntryField(state.sendTo, { state.sendTo = it; state.sendError = null },
            "收件人", "name@example.com", singleLine = true)
        EntryField(state.sendSubject, { state.sendSubject = it; state.sendError = null },
            "主题", "邮件主题", singleLine = true)
        EntryField(state.sendBody, { state.sendBody = it; state.sendError = null },
            "正文", "写点什么…", minLines = 12)
        state.sendError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatDetailTime(value: String?): String {
    if (value.isNullOrBlank()) return ""
    return runCatching {
        java.time.LocalDateTime.parse(value)
            .format(java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    }.getOrDefault("")
}

@Composable
fun ConfirmScreen(state: AssistantState) {
    PageColumn(spacing = 20.dp) {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(Modifier.size(64.dp), CircleShape, color = if (state.sendComplete) SoftGreen else SoftGray) {
                Box(contentAlignment = Alignment.Center) {
                    AppIcon(if (state.sendComplete) Glyph.Check else Glyph.Send, Modifier.size(28.dp),
                        tint = if (state.sendComplete) Green else Ink)
                }
            }
            Text(if (state.sendComplete) "模拟发送完成" else "发送回复？", style = MaterialTheme.typography.titleLarge)
            Text(if (state.sendComplete) "仅更新了本机状态" else "不会发出真实邮件",
                color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${state.selectedMail.sender} <${state.selectedMail.address}>", style = MaterialTheme.typography.bodySmall,
                color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("回复：${state.selectedMail.subject}", style = MaterialTheme.typography.bodySmall,
                color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp,
            bottomEnd = 20.dp, bottomStart = 6.dp), color = SoftGray, contentColor = Ink) {
            Text(state.draft, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
