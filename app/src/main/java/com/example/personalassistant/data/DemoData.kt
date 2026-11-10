package com.example.personalassistant.data

import java.time.LocalDate

data class Mail(
    val id: String, val sender: String, val address: String, val subject: String,
    val preview: String, val time: String, val body: String, val summary: String,
    val unread: Boolean = false, val waiting: Boolean = false,
    val important: Boolean = false, val archived: Boolean = false, val simulatedSent: Boolean = false
)

data class FoodRecord(
    val id: String, val meal: String, val food: String, val note: String,
    val time: String, val date: String = LocalDate.now().toString()
)

val mealNames = listOf("早餐", "午餐", "晚餐", "加餐")

fun demoMails() = listOf(
    Mail("project", "项目组", "project@example.com", "关于下阶段开发计划的讨论",
        "我们计划于本周五召开项目评审会，讨论下阶段的开发计划。", "09:20",
        "Hi 团队，\n\n我们计划于本周五召开项目评审会议，讨论下阶段的开发计划，主要包括：\n\n1. 功能范围的确认\n2. 时间安排与里程碑\n3. 资源协调\n\n请大家在会前准备相关材料，并在群内同步意见。\n\n谢谢！\n项目组",
        "本周五召开项目评审会；请提前准备功能范围、里程碑和资源需求，并同步意见。",
        unread = true, waiting = true, important = true),
    Mail("review", "李老师", "li@example.com", "季度例会时间协调",
        "关于本季度例会的时间安排，想听听你的建议。", "08:45",
        "你好，\n\n本季度例会暂定下周三下午 2 点举行。请确认你是否方便参加，也欢迎提出其他时间建议。\n\n祝好！\n李老师",
        "请确认能否参加下周三下午 2 点的季度例会，或提出其他时间建议。", unread = true, waiting = true),
    Mail("collab", "市场部", "team@example.com", "活动合作意向",
        "你好，我们希望与贵团队讨论本月的活动合作。", "昨天",
        "你好，\n\n我们正在筹备本月的线下交流活动，希望邀请贵团队参与。附件方案将在确认意向后提供，期待你的回复。\n\n市场部",
        "市场部邀请团队参与本月线下活动，需要回复合作意向。", waiting = true),
    Mail("product", "产品团队", "news@example.com", "订阅 · 每周产品动态",
        "本周为你带来产品更新、功能体验与设计灵感。", "昨天",
        "本周产品周刊\n\n新的工作台已上线，支持更清晰的任务视图和快捷操作。感谢你的关注，祝你拥有高效而轻松的一周。",
        "本周更新工作台与任务视图，无需回复。", unread = true),
    Mail("archive", "设计团队", "design@example.com", "上周设计评审纪要",
        "评审结论与待办已整理，供后续查阅。", "周一",
        "你好，\n\n上周的设计评审已结束。确认采用白色背景、轻灰卡片与绿色重点操作，后续按统一组件规范实现。",
        "设计规范已确认，评审纪要已归档。", archived = true)
)

fun demoFoods() = listOf(
    FoodRecord("sample-breakfast", "早餐", "全麦面包、鸡蛋、牛奶", "慢慢吃，开启新的一天", "08:10"),
    FoodRecord("sample-lunch", "午餐", "鸡胸肉、米饭、西兰花", "少油，搭配一份蔬菜", "12:20")
)

fun replyFor(mail: Mail, alternative: Boolean = false): String = if (alternative) {
    "${mail.sender}，你好：\n\n感谢来信，我已仔细阅读「${mail.subject}」。我会整理相关信息，并及时与你同步。如有补充材料，也欢迎随时发给我。\n\n谢谢！"
} else {
    when (mail.id) {
        "project" -> "你好，\n\n已收到关于下阶段开发计划的会议安排，感谢同步。\n\n我会在会前准备以下材料：\n1. 当前功能的进展与评估\n2. 下阶段的计划与排期建议\n3. 需要协调的资源与支持\n\n如有其他补充，请随时告知。\n\n谢谢！"
        "review" -> "李老师，您好：\n\n感谢通知，下周三下午 2 点我方便参加季度例会。我会提前准备相关材料。\n\n祝好！"
        else -> "${mail.sender}，你好：\n\n感谢你的来信。关于「${mail.subject}」，我已了解相关内容，会尽快整理意见并与你同步。\n\n如有进一步的安排，请随时告知。\n\n谢谢！"
    }
}

fun matchesMail(mail: Mail, query: String, filter: String): Boolean {
    val inFilter = when (filter) {
        "未读" -> mail.unread && !mail.archived
        "待回复" -> mail.waiting && !mail.archived
        "已归档" -> mail.archived
        else -> !mail.archived
    }
    return inFilter && "${mail.sender} ${mail.address} ${mail.subject} ${mail.body}"
        .contains(query.trim(), ignoreCase = true)
}

fun validTime(value: String) = value.matches(Regex("(?:[01]\\d|2[0-3]):[0-5]\\d"))
