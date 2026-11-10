package com.example.personalassistant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.personalassistant.data.AssistantState
import com.example.personalassistant.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AdaptivePair(first: @Composable (Modifier) -> Unit, second: @Composable (Modifier) -> Unit) {
    val largeFont = LocalDensity.current.fontScale > 1.3f
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 320.dp || largeFont) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.fillMaxWidth()); second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.weight(1f)); second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun HomeScreen(state: AssistantState, onCommand: () -> Unit) {
    PageColumn {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppIcon(Glyph.Spark, tint = Green)
            Text("个人助理", style = MaterialTheme.typography.titleSmall)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("M月d日 · EEEE", Locale.CHINESE)),
                color = Muted, style = MaterialTheme.typography.bodyMedium)
            val greeting = when (LocalTime.now().hour) {
                in 5..10 -> "早上好"; in 11..13 -> "中午好"; in 14..17 -> "下午好"; else -> "晚上好"
            }
            Text(greeting, style = MaterialTheme.typography.headlineLarge)
        }
        OutlinedTextField(state.assistantInput, { state.assistantInput = it }, Modifier.fillMaxWidth(),
            placeholder = { Text("整理邮件、记录午餐", style = MaterialTheme.typography.bodyMedium) },
            shape = RoundedCornerShape(20.dp), minLines = 2, maxLines = 4,
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = SoftGray,
                focusedContainerColor = SoftGray, unfocusedBorderColor = Color.Transparent),
            trailingIcon = {
                IconButton(onClick = onCommand) { AppIcon(Glyph.Arrow, tint = Green, description = "执行演示指令") }
            })
        AdaptivePair(
            first = { QuickCard("整理邮件", "${state.waitingCount} 封邮件待回复", Glyph.Mail, it) { state.selectTab("邮件") } },
            second = { QuickCard("记录饮食", "今天已记录 ${state.mealCount} 餐", Glyph.Food, it) { state.openFood() } }
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("今日概览")
            Panel {
                OverviewRow(Glyph.Mail, "邮件", "未读 ${state.unreadCount} 封 · 待回复 ${state.waitingCount} 封") { state.selectTab("邮件") }
                HorizontalDivider(color = Line)
                OverviewRow(Glyph.Food, "饮食", "今日 ${state.todayFoods.size} 条记录") { state.selectTab("饮食") }
            }
        }
    }
}

@Composable
private fun QuickCard(title: String, subtitle: String, glyph: Glyph, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick, modifier, shape = RoundedCornerShape(20.dp), color = SoftGray, contentColor = Ink) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIcon(glyph, tint = Green)
            Spacer(Modifier.height(4.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
fun OverviewRow(glyph: Glyph, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconTile(glyph, green = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
        AppIcon(Glyph.Next, Modifier.size(18.dp), tint = Muted)
    }
}
