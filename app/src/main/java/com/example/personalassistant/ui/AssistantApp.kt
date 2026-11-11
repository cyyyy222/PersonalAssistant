package com.example.personalassistant.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import kotlinx.coroutines.delay
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
    val authRepository = remember { AuthRepository() }
    val mailRepository = remember { MailRepository() }
    var regenerateDialog by rememberSaveable { mutableStateOf(false) }
    val notify: (String) -> Unit = { message -> scope.launch { snackbar.currentSnackbarData?.dismiss(); snackbar.showSnackbar(message) } }
    val back: () -> Unit = { focus.clearFocus(); state.back() }

    val performLogin: () -> Unit = {
        scope.launch {
            state.loginLoading = true
            state.loginError = null
            authRepository.login(state.loginAccount, state.loginPassword)
                .onSuccess { state.applySession(it) }
                .onFailure {
                    state.loginLoading = false
                    state.loginError = it.message ?: "登录失败"
                }
        }
    }
    val localNetworkPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) performLogin()
        else state.loginError = "需要允许访问本地网络"
    }

    if (!state.isLoggedIn) {
        LoginScreen(state) {
            when {
                state.loginAccount.isBlank() || state.loginPassword.isBlank() ->
                    state.loginError = "请输入账号或密码"
                state.loginLoading -> Unit
                Build.VERSION.SDK_INT >= 37 &&
                    context.checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) != PackageManager.PERMISSION_GRANTED -> {
                    state.loginError = "请允许访问本地网络"
                    localNetworkPermission.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                }
                else -> performLogin()
            }
        }
        return
    }

    BackHandler(enabled = state.page != "root" || state.tab != "首页", onBack = back)

    suspend fun handleMailError(error: Throwable, apply: (String) -> Unit) {
        if (error is MailApiException && error.status == 401) {
            state.logout()
        } else {
            apply(error.message ?: "请求失败")
        }
    }

    suspend fun loadRemoteMessages() {
        val account = state.mailAccount ?: return
        state.mailLoading = true
        state.mailError = null
        mailRepository.listMessages(
            state.authToken, account.id, 1, 50, state.mailQuery, state.mailUnreadOnly
        ).onSuccess { page ->
            state.remoteMails = page.items
            state.remoteTotal = page.total
            state.remoteHasMore = page.hasMore
        }.onFailure { error ->
            handleMailError(error) { state.mailError = it }
        }
        state.mailLoading = false
    }

    suspend fun syncAndLoad() {
        val account = state.mailAccount ?: return
        state.mailSyncing = true
        state.mailError = null
        var syncError: String? = null
        mailRepository.sync(state.authToken, account.id).onFailure { error ->
            if (error is MailApiException && error.status == 401) {
                state.logout()
            } else {
                syncError = error.message ?: "同步失败"
            }
        }
        state.mailSyncing = false
        loadRemoteMessages()
        if (syncError != null && state.mailError == null) state.mailError = syncError
    }

    suspend fun loadAccountsAndMail() {
        state.mailAccountsLoading = true
        mailRepository.listAccounts(state.authToken).onSuccess { accounts ->
            state.mailAccount = accounts.firstOrNull()
        }.onFailure { error ->
            handleMailError(error) { state.mailError = it }
        }
        state.mailAccountsLoading = false
    }

    LaunchedEffect(state.tab, state.mailAccount?.id) {
        if (!state.demoMailMode && state.tab == "邮件") {
            when {
                state.mailAccount == null -> loadAccountsAndMail()
                state.remoteMails.isEmpty() -> syncAndLoad()
            }
        }
    }

    LaunchedEffect(state.mailQuery, state.mailUnreadOnly, state.mailAccount?.id) {
        if (!state.demoMailMode && state.mailAccount != null) {
            delay(350)
            loadRemoteMessages()
        }
    }

    LaunchedEffect(state.page, state.selectedRemoteId) {
        if (state.page == "remoteDetail") {
            val id = state.selectedRemoteId ?: return@LaunchedEffect
            state.remoteDetailLoading = true
            state.remoteDetailError = null
            state.remoteDetail = null
            mailRepository.getMessage(state.authToken, id).onSuccess { detail ->
                state.remoteDetail = detail
                if (!detail.read) {
                    mailRepository.markRead(state.authToken, id, true)
                    state.remoteDetail = detail.copy(read = true)
                    state.remoteMails = state.remoteMails.map {
                        if (it.id == id) it.copy(read = true) else it
                    }
                }
            }.onFailure { error ->
                handleMailError(error) { state.remoteDetailError = it }
            }
            state.remoteDetailLoading = false
        }
    }

    val bindMail: () -> Unit = {
        when {
            state.mailBindEmail.isBlank() || state.mailBindSecret.isBlank() ->
                state.mailBindError = "请输入邮箱和授权码"
            state.mailBindLoading -> Unit
            else -> scope.launch {
                state.mailBindLoading = true
                state.mailBindError = null
                mailRepository.bind(
                    state.authToken, state.mailBindEmail, state.mailBindSecret, state.mailBindDisplayName
                ).onSuccess { account ->
                    state.mailAccount = account
                    state.mailBindSecret = ""
                    state.page = "root"
                    state.tab = "邮件"
                    notify("邮箱已绑定")
                }.onFailure { error ->
                    handleMailError(error) { state.mailBindError = it }
                }
                state.mailBindLoading = false
            }
        }
    }

    val unbindMail: () -> Unit = {
        val account = state.mailAccount
        if (account != null) scope.launch {
            mailRepository.unbind(state.authToken, account.id).onSuccess {
                state.mailAccount = null
                state.remoteMails = emptyList()
                state.remoteDetail = null
                state.page = "root"
                notify("邮箱已解绑")
            }.onFailure { error ->
                handleMailError(error) { state.mailBindError = it }
            }
        }
    }

    val refreshMail: () -> Unit = {
        scope.launch {
            if (state.mailAccount == null) loadAccountsAndMail() else syncAndLoad()
        }
    }

    val composeMail: () -> Unit = {
        if (state.mailAccount == null) state.openMailAccount() else state.openCompose()
    }

    val sendMail: () -> Unit = {
        when {
            state.sendTo.isBlank() || state.sendSubject.isBlank() || state.sendBody.isBlank() ->
                state.sendError = "请填写收件人、主题和正文"
            state.sendLoading -> Unit
            else -> {
                val account = state.mailAccount
                if (account == null) {
                    state.sendError = "请先绑定邮箱"
                } else scope.launch {
                    state.sendLoading = true
                    state.sendError = null
                    mailRepository.send(
                        state.authToken, account.id, state.sendTo, state.sendSubject, state.sendBody
                    ).onSuccess {
                        state.sendBody = ""
                        state.page = "root"
                        notify("邮件已发送")
                    }.onFailure { error ->
                        handleMailError(error) { state.sendError = it }
                    }
                    state.sendLoading = false
                }
            }
        }
    }

    val toggleRemoteRead: () -> Unit = {
        val detail = state.remoteDetail
        val account = state.mailAccount
        if (detail != null && account != null) {
            val newRead = !detail.read
            scope.launch {
                mailRepository.markRead(state.authToken, detail.id, newRead).onSuccess {
                    state.remoteDetail = detail.copy(read = newRead)
                    state.remoteMails = state.remoteMails.map {
                        if (it.id == detail.id) it.copy(read = newRead) else it
                    }
                }.onFailure { error ->
                    handleMailError(error) { notify(it) }
                }
            }
        }
    }

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
                        "mailAccount" -> "绑定邮箱"
                        "remoteDetail" -> state.remoteDetail?.fromName?.ifBlank {
                            state.remoteDetail?.fromAddress.orEmpty()
                        }.orEmpty().ifBlank { "邮件" }
                        "compose" -> "写邮件"
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
                                "mailAccount" -> if (state.mailAccount == null) {
                                    PrimaryButton(if (state.mailBindLoading) "绑定中" else "绑定并测试",
                                        bindMail, Modifier.fillMaxWidth(), Glyph.Check,
                                        enabled = !state.mailBindLoading)
                                } else PrimaryButton("完成", back, Modifier.fillMaxWidth(), Glyph.Check)
                                "remoteDetail" -> AdaptivePair(
                                    first = {
                                        SecondaryButton(
                                            if (state.remoteDetail?.read == true) "标记未读" else "标记已读",
                                            toggleRemoteRead, it, Glyph.Check
                                        )
                                    },
                                    second = {
                                        PrimaryButton("回复", {
                                            state.remoteDetail?.let { detail ->
                                                state.openCompose(
                                                    detail.fromAddress,
                                                    if (detail.subject.startsWith("回复：")) detail.subject
                                                    else "回复：${detail.subject}"
                                                )
                                            }
                                        }, it, Glyph.Send, enabled = state.remoteDetail != null)
                                    }
                                )
                                "compose" -> PrimaryButton(if (state.sendLoading) "发送中" else "发送",
                                    sendMail, Modifier.fillMaxWidth(), Glyph.Send,
                                    enabled = !state.sendLoading)
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
                    "mailAccount" -> MailAccountScreen(state, unbindMail)
                    "remoteDetail" -> RemoteMailDetailScreen(state)
                    "compose" -> ComposeMailScreen(state)
                    "food" -> FoodEntryScreen(state)
                    else -> when (state.tab) {
                        "邮件" -> MailListScreen(
                            state = state,
                            onBind = { state.openMailAccount() },
                            onCompose = composeMail,
                            onRefresh = refreshMail,
                            onOpenRemote = { state.openRemoteDetail(it) },
                            onQueryChange = { state.mailQuery = it },
                            onUnreadChange = { state.mailUnreadOnly = it }
                        )
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

@Preview(name = "登录 / light", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun LoginPreview() { PersonalAssistantTheme { AssistantApp(remember { AssistantState(null) }) } }

@Preview(name = "vivo X100 / light", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun HomePreview() {
    PersonalAssistantTheme {
        AssistantApp(remember {
            AssistantState(null).apply {
                applySession(AuthSession("preview", AuthUser(1, "caiyangyang", "13800138000", "Cai Yangyang")))
            }
        })
    }
}

@Preview(name = "Compact / large text", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Composable
private fun CompactPreview() {
    PersonalAssistantTheme {
        AssistantApp(remember {
            AssistantState(null).apply {
                applySession(AuthSession("preview", AuthUser(1, "caiyangyang", "13800138000", "Cai Yangyang")))
            }
        })
    }
}
