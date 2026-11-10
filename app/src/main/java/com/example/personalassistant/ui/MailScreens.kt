package com.example.personalassistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.personalassistant.data.*
import com.example.personalassistant.ui.theme.*

private val mailFilters = listOf("全部", "未读", "待回复", "已归档")

@Composable
fun MailListScreen(state: AssistantState) {
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
    Row(Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(9.dp)).padding(2.dp)) {
        mailFilters.forEach { filter ->
            val active = filter == selected
            Surface(onClick = { onSelect(filter) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(7.dp),
                color = if (active) Color.White else Color.Transparent,
                contentColor = if (active) Ink else Muted) {
                Text(filter, Modifier.fillMaxWidth().padding(vertical = 5.dp), textAlign = TextAlign.Center,
                    maxLines = 1, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
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
