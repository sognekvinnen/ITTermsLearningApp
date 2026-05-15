package com.example.ittermslearningapp

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class TestActivityTest {

    private fun launchWithSession(): ActivityScenario<TestActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE).edit()
            .putInt("user_id", 1).putString("username", "testuser").apply()
        val intent = Intent(context, TestActivity::class.java).apply {
            putExtra("topic_id", 1)
            putExtra("level", 1)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActivityScenario.launch(intent)
    }

    private fun wordForm(activity: TestActivity, count: Int): String {
        val m = TestActivity::class.java
            .getDeclaredMethod("wordForm", Int::class.java)
            .apply { isAccessible = true }
        return m.invoke(activity, count) as String
    }

    private fun scoreColor(activity: TestActivity, percent: Int): Int {
        val m = TestActivity::class.java
            .getDeclaredMethod("scoreColor", Int::class.java)
            .apply { isAccessible = true }
        return m.invoke(activity, percent) as Int
    }

    private fun getSkippedCount(activity: TestActivity): Int {
        val m = TestActivity::class.java
            .getDeclaredMethod("getSkippedQuestionsCount")
            .apply { isAccessible = true }
        return m.invoke(activity) as Int
    }

    // Склонение для одного вопроса
    @Test
    fun wordForm_one_returnsVopros() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("вопрос", wordForm(activity, 1))
            }
        }
    }

    // Склонение для двух вопросов
    @Test
    fun wordForm_two_returnsVoprosa() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("вопроса", wordForm(activity, 2))
            }
        }
    }

    // Зелёный цвет для высокого процента
    @Test
    fun scoreColor_highPercent_returnsGreen() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val green = android.graphics.Color.parseColor("#2E7D32")
                assertEquals("100% должно давать зелёный цвет", green, scoreColor(activity, 100))
                assertEquals("70% должно давать зелёный цвет",  green, scoreColor(activity, 70))
            }
        }
    }

    // Начальное количество пропущенных вопросов
    @Test
    fun getSkippedQuestionsCount_atStart_equalsQuestionListSize() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val questionsField = TestActivity::class.java
                    .getDeclaredField("questionList").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                val questions = questionsField.get(activity) as List<*>
                val skipped = getSkippedCount(activity)
                assertEquals(
                    "В начале теста все вопросы должны быть пропущены",
                    questions.size, skipped
                )
            }
        }
    }

    // Исключение склонения для чисел 11-14
    @Test
    fun wordForm_eleven_returnsVoprosov_notVopros() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("вопросов", wordForm(activity, 11))
                assertNotEquals("wordForm(11) не должен быть 'вопрос'", "вопрос", wordForm(activity, 11))
            }
        }
    }

    // Красный цвет для низкого процента
    @Test
    fun scoreColor_lowPercent_returnsRed() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val red    = android.graphics.Color.parseColor("#B71C1C")
                val orange = android.graphics.Color.parseColor("#E65100")
                val green  = android.graphics.Color.parseColor("#2E7D32")
                val actual = scoreColor(activity, 39)
                assertEquals("39% должно давать красный цвет", red, actual)
                assertNotEquals("39% не должно быть оранжевым", orange, actual)
                assertNotEquals("39% не должно быть зелёным",   green,  actual)
            }
        }
    }

    // Склонение для числа 21
    @Test
    fun wordForm_twentyOne_returnsVopros_notVoprosov() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("вопрос", wordForm(activity, 21))
                assertNotEquals("wordForm(21) не должен быть 'вопросов'", "вопросов", wordForm(activity, 21))
            }
        }
    }
}