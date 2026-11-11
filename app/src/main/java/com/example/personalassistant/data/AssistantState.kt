package com.example.personalassistant.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Small, local-only demo store. No account, mail client, HTTP client or remote AI is used. */
class AssistantState(private val preferences: SharedPreferences?, snapshot: String? = null) {
    val mails = mutableStateListOf<Mail>()
    val foods = mutableStateListOf<FoodRecord>()
    val drafts = mutableStateMapOf<String, String>()
    var tab by mutableStateOf("首页")
    var page by mutableStateOf("root")
    var selectedId by mutableStateOf("project")
    var query by mutableStateOf("")
    var filter by mutableStateOf("全部")
    var assistantInput by mutableStateOf("")
    var draft by mutableStateOf("")
    var alternate by mutableStateOf(false)
    var showSummary by mutableStateOf(false)
    var sendComplete by mutableStateOf(false)
    var foodId by mutableStateOf("")
    var meal by mutableStateOf("晚餐")
    var food by mutableStateOf("")
    var note by mutableStateOf("")
    var time by mutableStateOf("")
    var foodDate by mutableStateOf(LocalDate.now().toString())
    var foodError by mutableStateOf(false)
    var loginMode by mutableStateOf("账号")
    var loginAccount by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var loginError by mutableStateOf<String?>(null)
    var loginLoading by mutableStateOf(false)
    var authToken by mutableStateOf("")
    var authUser by mutableStateOf<AuthUser?>(null)
    var demoMailMode by mutableStateOf(false)
    var mailAccount by mutableStateOf<MailAccount?>(null)
    var mailBindEmail by mutableStateOf("")
    var mailBindSecret by mutableStateOf("")
    var mailBindDisplayName by mutableStateOf("")
    var mailBindError by mutableStateOf<String?>(null)
    var mailBindLoading by mutableStateOf(false)
    var mailAccountsLoading by mutableStateOf(false)
    var remoteMails by mutableStateOf<List<RemoteMail>>(emptyList())
    var remoteTotal by mutableStateOf(0L)
    var remoteHasMore by mutableStateOf(false)
    var mailQuery by mutableStateOf("")
    var mailUnreadOnly by mutableStateOf(false)
    var mailLoading by mutableStateOf(false)
    var mailSyncing by mutableStateOf(false)
    var mailError by mutableStateOf<String?>(null)
    var selectedRemoteId by mutableStateOf<Long?>(null)
    var remoteDetail by mutableStateOf<RemoteMailDetail?>(null)
    var remoteDetailLoading by mutableStateOf(false)
    var remoteDetailError by mutableStateOf<String?>(null)
    var sendTo by mutableStateOf("")
    var sendSubject by mutableStateOf("")
    var sendBody by mutableStateOf("")
    var sendLoading by mutableStateOf(false)
    var sendError by mutableStateOf<String?>(null)

    val isLoggedIn: Boolean get() = authToken.isNotBlank() && authUser != null

    init {
        mails.addAll(demoMails())
        foods.addAll(demoFoods())
        val raw = snapshot ?: preferences?.getString("demo_store_v1", null)
        if (raw != null) runCatching { restore(JSONObject(raw)) }
    }

    val selectedMail: Mail get() = mails.first { it.id == selectedId }
    val todayFoods: List<FoodRecord> get() = foods.filter { it.date == LocalDate.now().toString() }.sortedByDescending { it.time }
    val mealCount: Int get() = todayFoods.map { it.meal }.distinct().size
    val waitingCount: Int get() = mails.count { it.waiting && !it.archived }
    val unreadCount: Int get() = mails.count { it.unread && !it.archived }

    fun applySession(session: AuthSession) {
        authToken = session.token
        authUser = session.user
        loginPassword = ""
        loginError = null
        loginLoading = false
        tab = "首页"
        page = "root"
        persist()
    }

    fun logout() {
        authToken = ""
        authUser = null
        loginPassword = ""
        loginError = null
        loginLoading = false
        mailAccount = null
        remoteMails = emptyList()
        remoteDetail = null
        selectedRemoteId = null
        mailError = null
        sendError = null
        tab = "首页"
        page = "root"
        persist()
    }

    fun openMailAccount() {
        mailBindError = null
        mailBindSecret = ""
        page = "mailAccount"
    }

    fun openRemoteDetail(messageId: Long) {
        selectedRemoteId = messageId
        remoteDetail = null
        remoteDetailError = null
        page = "remoteDetail"
    }

    fun openCompose(to: String = "", subject: String = "", body: String = "") {
        sendTo = to
        sendSubject = subject
        sendBody = body
        sendError = null
        page = "compose"
    }

    fun selectTab(value: String) { tab = value; page = "root" }

    fun openMail(id: String) {
        selectedId = id
        showSummary = false
        page = "detail"
        updateMail { it.copy(unread = false) }
    }

    fun updateMail(change: (Mail) -> Mail) {
        val index = mails.indexOfFirst { it.id == selectedId }
        mails[index] = change(mails[index])
        persist()
    }

    fun openDraft() {
        alternate = false
        draft = drafts[selectedId] ?: replyFor(selectedMail)
        sendComplete = false
        page = "draft"
    }

    fun saveDraft() { drafts[selectedId] = draft; persist() }

    fun simulateSend() {
        if (sendComplete || draft.isBlank()) return
        saveDraft()
        updateMail { it.copy(waiting = false, simulatedSent = true) }
        sendComplete = true
    }

    fun openFood(value: String = "晚餐", existing: FoodRecord? = null) {
        foodId = existing?.id.orEmpty()
        meal = existing?.meal ?: value
        food = existing?.food.orEmpty()
        note = existing?.note.orEmpty()
        time = existing?.time ?: LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        foodDate = existing?.date ?: LocalDate.now().toString()
        foodError = false
        page = "food"
    }

    fun saveFood(): Boolean {
        foodError = food.isBlank() || !validTime(time)
        if (foodError) return false
        val record = FoodRecord(foodId.ifEmpty { UUID.randomUUID().toString() }, meal, food.trim(), note.trim(), time, foodDate)
        val index = foods.indexOfFirst { it.id == record.id }
        if (index < 0) foods.add(record) else foods[index] = record
        persist()
        page = "root"
        return true
    }

    fun back() {
        when (page) {
            "confirm" -> if (sendComplete) { page = "detail" } else { page = "draft" }
            "draft" -> { saveDraft(); page = "detail" }
            "mailAccount", "remoteDetail", "compose" -> page = "root"
            "detail", "food" -> page = "root"
            else -> selectTab("首页")
        }
    }

    private fun persist() { preferences?.edit()?.putString("demo_store_v1", encode(false))?.apply() }

    fun encode(includeSession: Boolean): String = JSONObject().apply {
        put("mails", JSONArray().apply { mails.forEach { mail -> put(JSONObject().apply {
            put("id", mail.id); put("unread", mail.unread); put("waiting", mail.waiting)
            put("archived", mail.archived); put("sent", mail.simulatedSent)
        }) } })
        put("foods", JSONArray().apply { foods.forEach { record -> put(JSONObject().apply {
            put("id", record.id); put("meal", record.meal); put("food", record.food)
            put("note", record.note); put("time", record.time); put("date", record.date)
        }) } })
        put("drafts", JSONObject().apply { drafts.forEach { (id, body) -> put(id, body) } })
        put("authToken", authToken)
        authUser?.let { put("authUser", it.toJson()) }
        if (includeSession) put("session", JSONObject().apply {
            put("tab", tab); put("page", page); put("selectedId", selectedId)
            put("query", query); put("filter", filter); put("assistantInput", assistantInput)
            put("draft", draft); put("alternate", alternate); put("showSummary", showSummary)
            put("sendComplete", sendComplete); put("foodId", foodId); put("meal", meal)
            put("food", food); put("note", note); put("time", time); put("foodDate", foodDate)
            put("foodError", foodError)
        })
    }.toString()

    private fun restore(json: JSONObject) {
        json.optJSONArray("mails")?.let { values ->
            for (i in 0 until values.length()) {
                val value = values.getJSONObject(i)
                val index = mails.indexOfFirst { it.id == value.getString("id") }
                if (index >= 0) mails[index] = mails[index].copy(
                    unread = value.getBoolean("unread"), waiting = value.getBoolean("waiting"),
                    archived = value.getBoolean("archived"), simulatedSent = value.optBoolean("sent")
                )
            }
        }
        json.optJSONArray("foods")?.let { values ->
            val restored = (0 until values.length()).map { i -> values.getJSONObject(i).let {
                FoodRecord(it.getString("id"), it.getString("meal"), it.getString("food"),
                    it.getString("note"), it.getString("time"), it.getString("date"))
            } }
            foods.clear(); foods.addAll(restored)
        }
        json.optJSONObject("drafts")?.let { values -> values.keys().forEach { drafts[it] = values.getString(it) } }
        authToken = json.optString("authToken")
        authUser = json.optJSONObject("authUser")?.let { runCatching { AuthUser.fromJson(it) }.getOrNull() }
        if (authToken.isBlank() || authUser == null) {
            authToken = ""
            authUser = null
        }
        json.optJSONObject("session")?.let {
            tab = it.getString("tab"); page = it.getString("page"); selectedId = it.getString("selectedId")
            query = it.getString("query"); filter = it.getString("filter"); assistantInput = it.getString("assistantInput")
            draft = it.getString("draft"); alternate = it.getBoolean("alternate"); showSummary = it.getBoolean("showSummary")
            sendComplete = it.getBoolean("sendComplete"); foodId = it.getString("foodId"); meal = it.getString("meal")
            food = it.getString("food"); note = it.getString("note"); time = it.getString("time")
            foodDate = it.getString("foodDate"); foodError = it.getBoolean("foodError")
        }
    }

    companion object {
        fun saver(preferences: SharedPreferences?) = Saver<AssistantState, String>(
            save = { it.encode(true) }, restore = { AssistantState(preferences, it) }
        )
    }
}
