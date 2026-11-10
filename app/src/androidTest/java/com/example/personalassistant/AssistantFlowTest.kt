package com.example.personalassistant

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.example.personalassistant.data.AssistantState
import com.example.personalassistant.ui.AssistantApp
import com.example.personalassistant.ui.theme.PersonalAssistantTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class AssistantFlowTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var state: AssistantState

    private fun start(restoration: StateRestorationTester? = null) {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            state = rememberSaveable(saver = AssistantState.saver(null)) { AssistantState(null) }
            PersonalAssistantTheme { AssistantApp(state) }
        }
        if (restoration == null) rule.setContent(content) else restoration.setContent(content)
    }

    @Test fun mailDraftConfirmationAndSimulation() {
        start()
        rule.onNodeWithTag("tab_邮件").performClick()
        rule.onNodeWithTag("mail_project").performClick()
        rule.onNodeWithText("AI 总结", substring = false).performClick()
        rule.onNodeWithText("本周五召开项目评审会；请提前准备功能范围、里程碑和资源需求，并同步意见。").assertExists()
        rule.onNodeWithText("生成回复", substring = false).performClick()
        rule.onNodeWithTag("field_回复正文").performTextReplacement("这是一份经过编辑的演示回复。")
        rule.onNodeWithText("发送前确认").performClick()
        rule.onNodeWithText("不会发出真实邮件").assertExists()
        rule.onNodeWithText("返回修改").performClick()
        rule.onNodeWithTag("field_回复正文").assertTextContains("这是一份经过编辑的演示回复。")
        rule.onNodeWithText("发送前确认").performClick()
        rule.onNodeWithText("模拟发送", substring = false).performClick()
        rule.onNodeWithText("模拟发送完成").assertExists()
        rule.runOnIdle {
            assertTrue(state.selectedMail.simulatedSent)
            assertFalse(state.selectedMail.waiting)
            assertEquals("这是一份经过编辑的演示回复。", state.drafts["project"])
        }
        rule.onNodeWithText("返回邮件详情").performClick()
        rule.onNodeWithContentDescription("返回").performClick()
        rule.onNodeWithTag("tab_邮件").assertIsSelected()
    }

    @Test fun foodValidationSaveAndEdit() {
        start()
        rule.onNodeWithTag("tab_饮食").performClick()
        rule.onNodeWithContentDescription("新增饮食记录").performClick()
        rule.onNodeWithText("保存记录").performClick()
        rule.onNodeWithText("请先填写这一餐吃了什么").assertExists()
        rule.onNodeWithTag("field_食物内容 *").performTextInput("番茄鸡蛋面")
        rule.onNodeWithText("保存记录").performClick()
        rule.runOnIdle {
            assertEquals(3, state.todayFoods.size)
            assertEquals(3, state.mealCount)
            state.openFood(existing = state.foods.last())
        }
        rule.onNodeWithTag("field_食物内容 *").performTextReplacement("番茄鸡蛋面、水果")
        rule.onNodeWithText("保存记录").performClick()
        rule.runOnIdle {
            assertEquals(3, state.todayFoods.size)
            assertEquals("番茄鸡蛋面、水果", state.foods.last().food)
        }
        rule.onNodeWithTag("tab_饮食").assertIsSelected()
    }

    @Test fun restoresUnsavedFormAndNavigation() {
        val restoration = StateRestorationTester(rule)
        start(restoration)
        rule.runOnIdle { state.openFood("午餐") }
        rule.onNodeWithTag("field_食物内容 *").performTextInput("咖喱饭")
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("field_食物内容 *").assertTextContains("咖喱饭")
        rule.runOnIdle { assertEquals("午餐", state.meal); assertEquals("food", state.page) }
        rule.onNodeWithContentDescription("返回").performClick()
        rule.onNodeWithTag("tab_首页").assertIsSelected()
    }

    @Test fun searchEmptyStateAndArchive() {
        start()
        rule.onNodeWithTag("tab_邮件").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("不存在的主题")
        rule.onNodeWithText("没有找到邮件").assertExists()
        rule.onNodeWithText("查看全部").performClick()
        rule.onNodeWithTag("mail_project").performClick()
        rule.onNodeWithContentDescription("归档").performClick()
        rule.onNodeWithContentDescription("返回").performClick()
        rule.onNodeWithTag("mail_project").assertDoesNotExist()
        rule.runOnIdle { state.filter = "已归档" }
        rule.onNodeWithTag("mail_project").assertExists()
    }

    @Test fun captureSevenScreensForVisualReview() {
        start()
        fun capture(name: String) {
            rule.waitForIdle()
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val directory = instrumentation.targetContext.getExternalFilesDir("screenshots")!!
            directory.mkdirs()
            val bitmap = rule.onRoot().captureToImage().asAndroidBitmap()
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
        capture("01-home")
        rule.runOnIdle { state.selectTab("邮件") }; capture("02-mail")
        rule.runOnIdle { state.openMail("project") }; capture("03-detail")
        rule.runOnIdle { state.openDraft() }; capture("04-draft")
        rule.runOnIdle { state.page = "confirm" }; capture("05-confirm")
        rule.runOnIdle { state.selectTab("饮食") }; capture("06-food")
        rule.runOnIdle { state.openFood() }; capture("07-food-entry")
    }
}
