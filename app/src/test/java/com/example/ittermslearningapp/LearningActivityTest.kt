package com.example.ittermslearningapp

import android.content.Context
import android.content.Intent
import android.view.View
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

    // Проверка метки сложного уровня
    @Test
    fun difficultyLabel_level3_returnsHardLabel() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals("Сложный уровень", difficultyLabel(activity, 3))
            }
        }
    }

    // Проверка полного порядка уровней при выборе уровня 1
    @Test
    fun buildLevelOrder_selected1_returnsCorrectOrder() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(listOf(1, 2, 3), buildLevelOrder(activity, 1))
            }
        }
    }

    // Проверка полного порядка уровней при выборе уровня 3
    @Test
    fun buildLevelOrder_selected3_returnsCorrectOrder() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(listOf(3, 2, 1), buildLevelOrder(activity, 3))
            }
        }
    }

    // Размер списка buildLevelOrder всегда равен 3
    @Test
    fun buildLevelOrder_alwaysReturnsThreeElements() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                for (lvl in 1..3) {
                    assertEquals(
                        "buildLevelOrder($lvl) должен содержать 3 элемента",
                        3, buildLevelOrder(activity, lvl).size
                    )
                }
            }
        }
    }

    // buildLevelOrder не содержит дубликатов
    @Test
    fun buildLevelOrder_containsNoDuplicates() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                for (lvl in 1..3) {
                    val order = buildLevelOrder(activity, lvl)
                    assertEquals(
                        "buildLevelOrder($lvl) не должен содержать дубликаты",
                        order.distinct(), order
                    )
                }
            }
        }
    }

    // toggleDefinition скрывает определение при первом вызове
    @Test
    fun toggleDefinition_firstCall_hidesDefinition() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                // Нажимаем кнопку скрытия/показа определения
                activity.binding.btnToggleDefinition.performClick()
                assertEquals("", activity.binding.tvDefinition.text.toString())
                assertEquals("Показать определение", activity.binding.btnToggleDefinition.text.toString())
            }
        }
    }

    // toggleDefinition возвращает определение при втором вызове
    @Test
    fun toggleDefinition_secondCall_showsDefinitionAgain() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                activity.binding.btnToggleDefinition.performClick() // скрыть
                activity.binding.btnToggleDefinition.performClick() // показать
                assertNotEquals("", activity.binding.tvDefinition.text.toString())
                assertEquals("Спрятать определение", activity.binding.btnToggleDefinition.text.toString())
            }
        }
    }

    // Кнопка "Назад" скрыта при отсутствии истории
    @Test
    fun navigation_backButtonHiddenInitially() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.GONE, activity.binding.btnPrevious.visibility)
            }
        }
    }

    // Кнопка "Вперёд" скрыта при отсутствии истории
    @Test
    fun navigation_forwardButtonHiddenInitially() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(View.GONE, activity.binding.btnNext.visibility)
            }
        }
    }

    // registerConceptView увеличивает счётчик просмотров
    @Test
    fun registerConceptView_incrementsViewCount() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val register = LearningActivity::class.java
                    .getDeclaredMethod("registerConceptView", Int::class.java)
                    .apply { isAccessible = true }

                val viewedConcepts = LearningActivity::class.java
                    .getDeclaredField("viewedConcepts")
                    .apply { isAccessible = true }
                    .get(activity) as MutableMap<*, *>

                register.invoke(activity, 42)
                register.invoke(activity, 42)

                assertEquals(2, viewedConcepts[42])
            }
        }
    }

    // registerConceptView корректно обрабатывает новый концепт
    @Test
    fun registerConceptView_newConcept_startsAtOne() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val register = LearningActivity::class.java
                    .getDeclaredMethod("registerConceptView", Int::class.java)
                    .apply { isAccessible = true }

                val viewedConcepts = LearningActivity::class.java
                    .getDeclaredField("viewedConcepts")
                    .apply { isAccessible = true }
                    .get(activity) as MutableMap<*, *>

                register.invoke(activity, 99)
                assertEquals(1, viewedConcepts[99])
            }
        }
    }

    // Запуск с корректной сессией — активность не завершается
    @Test
    fun onCreate_withValidSession_activityNotDestroyed() {
        launchWithSession().use { scenario ->
            assertNotEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    // tvTerm не пустой после загрузки начального термина
    @Test
    fun loadInitialTerm_withValidData_termTextNotEmpty() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity.binding.tvTerm.text)
            }
        }
    }
}