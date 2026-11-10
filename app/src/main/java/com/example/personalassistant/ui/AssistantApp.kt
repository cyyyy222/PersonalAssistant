package com.example.personalassistant.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.personalassistant.data.*
import com.example.personalassistant.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AssistantApp(stateOverride: AssistantState? = null) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("assistant_demo", Context.MODE_PRIVATE) }
    val savedState = rememberSaveable(saver = AssistantState.saver(preferences)) { AssistantState(preferences) }
    val state = stateOverride ?: savedState
    val pageStates = rememberSaveableStateHolder()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var regenerateDialog by rememberSaveable { mutableStateOf(false) }
    val notify: (String) -> Unit = { message -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(message) } }
    val back: () -> Unit = { focus.clearFocus(); state.back() }
    BackHandler(enabled = state.page != "root" || state.tab != "首页", onBack = back)

    if (regenerateDialog) AlertDialog(onDismissRequest = { regenerateDialog = false },
        title = { Text("重新生成回复？") }, text = { Text("当前正文将被替换。") },
        confirmButton = { TextButton(onClick = {
            state.alternate = !state.alternate; state.draft = replyFor(state.selectedMail, state.alternate)
            regenerateDialog = false; notify("已替换回复")
        }) { Text("替换正文") } },
        dismissButton = { TextButton(onClick = { regenerateDialog = false }) { Text("取消") } })

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(), containerColor = Color.White,
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout),
        topBar = {
            if (state.page != "root") Centered {
                Box(Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 60.dp)) {
                    Text(when (state.page) {
                        "detail" -> state.selectedMail.sender
                        "draft" -> "回复"
                        "confirm" -> "确认"
                        else -> if (state.foodId.isEmpty()) "记录" else "编辑"
                    }, Modifier.align(Alignment.Center).padding(horizontal = 104.dp),
                        style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = back) { AppIcon(Glyph.Back, description = "返回") }
                        Spacer(Modifier.weight(1f))
                        if (state.page == "detail") {
                            IconButton(onClick = {
                                state.updateMail { it.copy(waiting = !it.waiting) }
                                notify(if (state.selectedMail.waiting) "已标记待回复" else "已取消")
                            }) {
                                AppIcon(Glyph.Clock, tint = if (state.selectedMail.waiting) Green else Ink,
                                    description = if (state.selectedMail.waiting) "取消待回复" else "标记待回复")
                            }
                            IconButton(onClick = {
                                val archived = state.selectedMail.archived
                                state.updateMail { it.copy(archived = !archived) }
                                notify(if (archived) "已移回收件箱" else "已归档")
                            }) {
                                AppIcon(Glyph.Archive, tint = if (state.selectedMail.archived) Green else Ink,
                                    description = if (state.selectedMail.archived) "移回收件箱" else "归档")
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(Modifier.navigationBarsPadding()) {
                HorizontalDivider(color = Line)
                Centered {
                    if (state.page == "root") BottomTabs(state) else {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (state.page) {
                                "detail" -> AdaptivePair(
                                    first = { SecondaryButton(if (state.showSummary) "收起总结" else "AI 总结", { state.showSummary = !state.showSummary }, it, Glyph.Spark) },
                                    second = { PrimaryButton("生成回复", { state.openDraft() }, it, Glyph.Spark) }
                                )
                                "draft" -> PrimaryButton("发送前确认", {
                                    focus.clearFocus(); state.saveDraft(); state.sendComplete = false; state.page = "confirm"
                                }, Modifier.fillMaxWidth(), Glyph.Send, enabled = state.draft.isNotBlank())
                                "confirm" -> if (state.sendComplete) {
                                    PrimaryButton("返回邮件详情", { state.page = "detail" }, Modifier.fillMaxWidth(), Glyph.Check)
                                } else AdaptivePair(
                                    first = { SecondaryButton("返回修改", back, it) },
                                    second = { PrimaryButton("模拟发送", { state.simulateSend() }, it, Glyph.Send, enabled = state.draft.isNotBlank()) }
                                )
                                "food" -> PrimaryButton("保存记录", {
                                    if (state.saveFood()) { focus.clearFocus(); notify("已保存") }
                                    else notify("请填写内容和时间")
                                }, Modifier.fillMaxWidth(), Glyph.Check)
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                pageStates.SaveableStateProvider("${state.page}_${state.tab}") {
                when (state.page) {
                    "detail" -> MailDetailScreen(state)
                    "draft" -> DraftScreen(state, { regenerateDialog = true }, { state.saveDraft(); notify("草稿已保存") })
                    "confirm" -> ConfirmScreen(state)
                    "food" -> FoodEntryScreen(state)
                    else -> when (state.tab) {
                        "邮件" -> MailListScreen(state)
                        "饮食" -> FoodScreen(state)
                        else -> HomeScreen(state) {
                            val command = state.assistantInput.trim()
                            when {
                                command.contains("邮件") || command.contains("邮箱") -> { focus.clearFocus(); state.selectTab("邮件"); state.assistantInput = "" }
                                listOf("饮食", "早餐", "午餐", "晚餐", "加餐", "吃").any { command.contains(it) } -> {
                                    focus.clearFocus(); state.openFood(mealNames.firstOrNull { command.contains(it) } ?: "晚餐"); state.assistantInput = ""
                                }
                                else -> notify("试试“整理邮件”或“记录午餐”")
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = 640.dp).fillMaxWidth()) { content() }
    }
}

@Composable
private fun BottomTabs(state: AssistantState) {
    NavigationBar(containerColor = Color.White, tonalElevation = 0.dp, windowInsets = WindowInsets(0, 0, 0, 0)) {
        listOf("首页" to Glyph.Home, "邮件" to Glyph.Mail, "饮食" to Glyph.Food).forEach { (title, icon) ->
            NavigationBarItem(selected = state.tab == title, onClick = { state.selectTab(title) }, modifier = Modifier.testTag("tab_$title"),
                icon = { AppIcon(icon, tint = if (state.tab == title) Green else Muted) }, label = { Text(title) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent,
                    selectedTextColor = Green, unselectedTextColor = Muted))
        }
    }
}

@Preview(name = "vivo X100 / light", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun HomePreview() { PersonalAssistantTheme { AssistantApp(remember { AssistantState(null) }) } }

@Preview(name = "Compact / large text", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun CompactPreview() { PersonalAssistantTheme { AssistantApp(remember { AssistantState(null) }) } }
