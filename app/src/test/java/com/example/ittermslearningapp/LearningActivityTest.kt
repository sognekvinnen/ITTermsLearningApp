package com.example.ittermslearningapp

import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LearningActivityTest {

    private fun launchWithSession(topicId: Int = 1, level: Int = 1): ActivityScenario<LearningActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE).edit()
            .putInt("user_id", 1)
            .putString("username", "testuser")
            .apply()
        val intent = Intent(context, LearningActivity::class.java).apply {
            putExtra("topic_id", topicId)
            putExtra("level", level)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return ActivityScenario.launch(intent)
    }

    private fun difficultyLabel(activity: LearningActivity, difficulty: Int): String {
        val m = LearningActivity::class.java
            .getDeclaredMethod("difficultyLabel", Int::class.java)
            .apply { isAccessible = true }
        return m.invoke(activity, difficulty) as String
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildLevelOrder(activity: LearningActivity, selected: Int): List<Int> {
        val m = LearningActivity::class.java
            .getDeclaredMethod("buildLevelOrder", Int::class.java)
            .apply { isAccessible = true }
        return m.invoke(activity, selected) as List<Int>
    }

    // Проверка метки простого уровня
    @Test
    fun difficultyLabel_level1_returnsEasyLabel() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Простой уровень", difficultyLabel(activity, 1))
            }
        }
    }

    // Проверка метки среднего уровня
    @Test
    fun difficultyLabel_level2_returnsMediumLabel() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Средний уровень", difficultyLabel(activity, 2))
            }
        }
    }

    // Проверка порядка приоритета уровней
    @Test
    fun buildLevelOrder_selected2_returnsCorrectPriorityOrder() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(listOf(2, 1, 3), buildLevelOrder(activity, 2))
            }
        }
    }

    // Проверка первого элемента в порядке
    @Test
    fun buildLevelOrder_selected3_firstElementIsSelected() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val order = buildLevelOrder(activity, 3)
                assertEquals("Первым должен быть выбранный уровень", 3, order.first())
            }
        }
    }

    // Обработка несуществующего уровня
    @Test
    fun difficultyLabel_unknownLevel_returnsFallbackString() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Уровень 5", difficultyLabel(activity, 5))
            }
        }
    }

    // Обработка нулевого уровня
    @Test
    fun difficultyLabel_levelZero_returnsFallback() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Уровень 0", difficultyLabel(activity, 0))
            }
        }
    }

    // Запуск без идентификатора пользователя
    @Test
    fun onCreate_missingUserId_activityFinishes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .edit().clear().apply()

        val intent = Intent(context, LearningActivity::class.java).apply {
            putExtra("topic_id", 1)
            putExtra("level", 1)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ActivityScenario.launch<LearningActivity>(intent).use { scenario ->
            assertEquals(
                "Активность должна завершиться при отсутствии userId",
                Lifecycle.State.DESTROYED, scenario.state
            )
        }
    }
}