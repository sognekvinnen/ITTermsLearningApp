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
class MyProgressActivityTest {

    private fun makeItem(
        term: String       = "Term",
        difficulty: Int    = 1,
        status: String     = "Изучаю",
        number: Int        = 0,
        topicName: String  = "Тема"
    ) = MyProgressActivity.ProgressItem(
        term = term, difficulty = difficulty, topicId = 1,
        topicName = topicName, status = status, number = number
    )

    @Suppress("UNCHECKED_CAST")
    private fun applySort(
        activity: MyProgressActivity,
        list: List<MyProgressActivity.ProgressItem>,
        column: MyProgressActivity.SortColumn,
        ascending: Boolean
    ): List<MyProgressActivity.ProgressItem> {
        val field = MyProgressActivity::class.java
            .getDeclaredField("sortState").apply { isAccessible = true }
        field.set(activity, MyProgressActivity.SortState(column, ascending))

        val method = MyProgressActivity::class.java
            .getDeclaredMethod("applySort", List::class.java).apply { isAccessible = true }
        return method.invoke(activity, list) as List<MyProgressActivity.ProgressItem>
    }

    private fun launchWithSession(): ActivityScenario<MyProgressActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE).edit()
            .putInt("user_id", 1).putString("username", "testuser").apply()
        return ActivityScenario.launch(MyProgressActivity::class.java)
    }

    // Сортировка терминов по возрастанию
    @Test
    fun applySort_byTermAscending_producesAlphabeticOrder() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val items = listOf(makeItem("Zebra"), makeItem("Apple"), makeItem("Mango"))
                val sorted = applySort(activity, items, MyProgressActivity.SortColumn.TERM, ascending = true)
                assertEquals("Apple", sorted[0].term)
                assertEquals("Mango", sorted[1].term)
                assertEquals("Zebra", sorted[2].term)
            }
        }
    }

    // Сортировка статусов по возрастанию
    @Test
    fun applySort_byStatusAscending_respectsStatusOrder() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val items = listOf(
                    makeItem(status = "Знаю"),
                    makeItem(status = "Изучаю"),
                    makeItem(status = "Повторяю")
                )
                val sorted = applySort(activity, items, MyProgressActivity.SortColumn.STATUS, ascending = true)
                assertEquals("Изучаю",   sorted[0].status)
                assertEquals("Повторяю", sorted[1].status)
                assertEquals("Знаю",     sorted[2].status)
            }
        }
    }

    // Сохранение исходного порядка без сортировки
    @Test
    fun applySort_byNone_preservesOriginalOrder() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val items = listOf(makeItem("C"), makeItem("A"), makeItem("B"))
                val sorted = applySort(activity, items, MyProgressActivity.SortColumn.NONE, ascending = true)
                assertEquals("C", sorted[0].term)
                assertEquals("A", sorted[1].term)
                assertEquals("B", sorted[2].term)
            }
        }
    }

    // Сортировка по столбцу повторений по убыванию
    @Test
    fun applySort_byRepetitionsDescending_maxRepetitionsFirst() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val items = listOf(makeItem(number = 5), makeItem(number = 10), makeItem(number = 1))
                val sorted = applySort(activity, items, MyProgressActivity.SortColumn.REPETITIONS, ascending = false)
                assertEquals(10, sorted[0].number)
            }
        }
    }

    // Сортировка сложности по убыванию
    @Test
    fun applySort_byDifficultyDescending_highestDifficultyFirst() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val items = listOf(
                    makeItem(difficulty = 1),
                    makeItem(difficulty = 3),
                    makeItem(difficulty = 2)
                )
                val sorted = applySort(activity, items, MyProgressActivity.SortColumn.DIFFICULTY, ascending = false)
                assertEquals("Элемент с difficulty=3 должен быть первым", 3, sorted[0].difficulty)
            }
        }
    }

    // Сортировка пустого списка
    @Test
    fun applySort_emptyList_returnsEmptyListWithoutException() {
        launchWithSession().use { scenario ->
            scenario.onActivity { activity ->
                val result = applySort(activity, emptyList(), MyProgressActivity.SortColumn.TERM, ascending = true)
                assertTrue("Сортировка пустого списка должна вернуть пустой список", result.isEmpty())
            }
        }
    }

    // Запуск с некорректным userId
    @Test
    fun onCreate_invalidUserId_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .edit().putInt("user_id", -1).apply()
        ActivityScenario.launch(MyProgressActivity::class.java).use { scenario ->
            assertNotEquals(
                "Активность должна запускаться даже при некорректном userId",
                Lifecycle.State.DESTROYED, scenario.state
            )
        }
    }
}