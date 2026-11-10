package com.example.personalassistant

import com.example.personalassistant.data.*
import org.junit.Assert.*
import org.junit.Test

class DemoLogicTest {
    @Test fun searchCombinesFilterAndQuery() {
        val mail = demoMails().first()
        assertTrue(matchesMail(mail, " PROJECT@EXAMPLE.COM ", "待回复"))
        assertFalse(matchesMail(mail, "不存在的主题", "待回复"))
        assertFalse(matchesMail(mail.copy(archived = true), "", "未读"))
        assertFalse(matchesMail(mail.copy(archived = true), "", "全部"))
        assertTrue(matchesMail(mail.copy(archived = true), "开发", "已归档"))
        assertFalse(matchesMail(mail.copy(waiting = false), "", "待回复"))
    }

    @Test fun acceptsOnly24HourTimes() {
        listOf("00:00", "08:10", "23:59").forEach { assertTrue(it, validTime(it)) }
        listOf("24:00", "12:60", "9:5", "", "-1:30").forEach { assertFalse(it, validTime(it)) }
    }

    @Test fun regeneratedReplyIsDifferentAndReferencesSelectedMail() {
        val mail = demoMails()[1]
        assertNotEquals(replyFor(mail), replyFor(mail, true))
        assertTrue(replyFor(mail).contains("李老师"))
        assertTrue(replyFor(mail, true).contains(mail.subject))
    }
}
