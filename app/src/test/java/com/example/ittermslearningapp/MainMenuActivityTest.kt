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
class MainMenuActivityTest {

    private fun launchWithUser(username: String = "testuser"): ActivityScenario<MainMenuActivity> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE).edit()
            .putInt("user_id", 1)
            .putString("username", username)
            .apply()
        return ActivityScenario.launch(MainMenuActivity::class.java)
    }

    // Проверка имени в приветствии
    @Test
    fun onCreate_welcomeText_containsUsername() {
        launchWithUser("Valeria").use { scenario ->
            scenario.onActivity { activity ->
                val tv = activity.findViewById<TextView>(R.id.tvMainMenuWelcome)
                assertTrue(
                    "Приветствие должно содержать имя пользователя",
                    tv.text.contains("Valeria")
                )
            }
        }
    }

    // Проверка названия приложения
    @Test
    fun onCreate_welcomeText_containsAppName() {
        launchWithUser().use { scenario ->
            scenario.onActivity { activity ->
                val tv = activity.findViewById<TextView>(R.id.tvMainMenuWelcome)
                assertTrue("Приветствие должно содержать 'IT Atlas'", tv.text.contains("IT Atlas"))
            }
        }
    }

    // Видимость кнопки обучения
    @Test
    fun onCreate_learningButton_isEnabledAndVisible() {
        launchWithUser().use { scenario ->
            scenario.onActivity { activity ->
                val btn = activity.findViewById<Button>(R.id.btnLearning)
                assertEquals(android.view.View.VISIBLE, btn.visibility)
                assertTrue("Кнопка 'Обучение' должна быть активна", btn.isEnabled)
            }
        }
    }

    // Видимость кнопки теста
    @Test
    fun onCreate_testButton_isEnabledAndVisible() {
        launchWithUser().use { scenario ->
            scenario.onActivity { activity ->
                val btn = activity.findViewById<Button>(R.id.btnTest)
                assertEquals(android.view.View.VISIBLE, btn.visibility)
                assertTrue("Кнопка 'Тест' должна быть активна", btn.isEnabled)
            }
        }
    }

    // Отсутствие имени пользователя
    @Test
    fun onCreate_noUsernameInSession_showsDefaultPlaceholder() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
            .edit().clear().apply()
        ActivityScenario.launch(MainMenuActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val tv = activity.findViewById<TextView>(R.id.tvMainMenuWelcome)
                assertTrue(
                    "При отсутствии имени должен отображаться placeholder 'Пользователь'",
                    tv.text.contains("Пользователь")
                )
            }
        }
    }

    // Блокировка навигации без выбора темы
    @Test
    fun testButton_click_doesNotNavigateImmediatelyToTestActivity() {
        launchWithUser().use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnTest).performClick()
                assertNotEquals(
                    "После клика 'Тест' активность не должна быть уничтожена",
                    Lifecycle.State.DESTROYED, scenario.state
                )
            }
        }
    }

    // Обработка пустого имени
    @Test
    fun onCreate_emptyUsernameString_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE).edit()
            .putInt("user_id", 1)
            .putString("username", "")
            .apply()
        ActivityScenario.launch(MainMenuActivity::class.java).use { scenario ->
            assertNotEquals(
                "Активность не должна упасть при пустом имени пользователя",
                Lifecycle.State.DESTROYED, scenario.state
            )
        }
    }
}