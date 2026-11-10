package com.example.personalassistant.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.personalassistant.data.*
import com.example.personalassistant.ui.theme.*
import java.time.LocalDate
import java.util.Locale

@Composable
fun FoodScreen(state: AssistantState) {
    PageColumn {
        PageHeading("饮食") {
            IconButton(onClick = { state.openFood() }) { AppIcon(Glyph.Plus, description = "新增饮食记录") }
        }
        Panel {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("今日记录", style = MaterialTheme.typography.titleMedium)
                    Text("${state.mealCount} 类餐次", style = MaterialTheme.typography.headlineMedium)
                    Text("${state.todayFoods.size} 条记录", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { state.mealCount / 4f }, Modifier.fillMaxSize(),
                        color = Green, trackColor = Line, strokeWidth = 5.dp)
                    Text("${state.mealCount}/4", style = MaterialTheme.typography.titleSmall)
                }
            }
            HorizontalDivider(color = Line)
            mealNames.forEach { meal ->
                val recorded = state.todayFoods.any { it.meal == meal }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppIcon(mealGlyph(meal), Modifier.size(20.dp), if (recorded) Green else Muted)
                    Text(meal, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(if (recorded) "已记录" else "待记录", color = if (recorded) Green else Muted,
                        style = MaterialTheme.typography.bodySmall)
                    if (recorded) AppIcon(Glyph.Check, Modifier.size(18.dp), Green) else Spacer(Modifier.width(18.dp))
                }
            }
        }
        PrimaryButton("记录饮食", { state.openFood() }, Modifier.fillMaxWidth(), Glyph.Plus)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                mealNames.forEach { meal ->
                    SuggestionChip(onClick = { state.openFood(meal) }, label = { Text(meal) },
                        icon = { AppIcon(mealGlyph(meal), Modifier.size(18.dp), Green) }, shape = RoundedCornerShape(12.dp))
                }
            }
            state.todayFoods.forEach { record ->
                Surface(onClick = { state.openFood(existing = record) }, shape = RoundedCornerShape(18.dp), color = SoftGray, contentColor = Ink) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        IconTile(mealGlyph(record.meal), green = true)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(record.meal, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                Text(record.time, style = MaterialTheme.typography.bodySmall, color = Muted)
                            }
                            Text(record.food, style = MaterialTheme.typography.bodyLarge)
                            if (record.note.isNotBlank()) Text(record.note, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (state.todayFoods.isEmpty()) EmptyState("今天还没有记录", action = "记录第一餐",
                onClick = { state.openFood("早餐") }, glyph = Glyph.Food)
        }
    }
}

@Composable
fun FoodEntryScreen(state: AssistantState) {
    val context = LocalContext.current
    PageColumn {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("选择餐次", style = MaterialTheme.typography.titleSmall)
            // Scroll instead of squeezing four labels when accessibility font size increases.
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                mealNames.forEach { value ->
                    val selected = state.meal == value
                    FilterChip(selected, { state.meal = value }, label = {
                        Column(Modifier.padding(horizontal = 6.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppIcon(mealGlyph(value), tint = if (selected) Color.White else Ink)
                            Text(value)
                        }
                    }, colors = FilterChipDefaults.filterChipColors(containerColor = SoftGray,
                        selectedContainerColor = Green, selectedLabelColor = Color.White),
                        border = null, shape = RoundedCornerShape(16.dp))
                }
            }
        }
        EntryField(state.food, { state.food = it }, "食物内容 *", "例如：米饭、鸡胸肉、西兰花", minLines = 4,
            error = state.foodError && state.food.isBlank(),
            supporting = if (state.foodError && state.food.isBlank()) "请先填写这一餐吃了什么" else null)
        EntryField(state.note, { state.note = it }, "备注（选填）", "口味、心情…", minLines = 3)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("时间", style = MaterialTheme.typography.titleSmall)
            Surface(onClick = {
                val parts = state.time.split(":")
                TimePickerDialog(context, { _, hour, minute ->
                    state.time = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                }, parts.getOrNull(0)?.toIntOrNull() ?: 12, parts.getOrNull(1)?.toIntOrNull() ?: 0, true).show()
            }, shape = RoundedCornerShape(16.dp), color = SoftGray, contentColor = Ink) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AppIcon(Glyph.Clock)
                    Text("${if (state.foodDate == LocalDate.now().toString()) "今天" else state.foodDate}  ${state.time}", Modifier.weight(1f))
                    AppIcon(Glyph.Next, Modifier.size(18.dp), Muted)
                }
            }
            if (state.foodError && !validTime(state.time)) Text("请选择有效时间", color = MaterialTheme.colorScheme.error)
        }
    }
}
