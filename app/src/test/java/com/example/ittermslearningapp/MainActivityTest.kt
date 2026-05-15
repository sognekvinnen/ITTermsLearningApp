package com.example.ittermslearningapp

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class MainActivityTest {

    // Пользователь уже авторизован — переход на MainMenuActivity
    @Test
    fun mainActivity_userAlreadyLoggedIn_navigatesToMainMenu() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.edit().putInt("user_id", 42).commit()

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val shadowApplication = Shadows.shadowOf(
                    ApplicationProvider.getApplicationContext<Application>()
                )
                val startedIntent = shadowApplication.nextStartedActivity
                assertNotNull("Должен быть запущен Intent", startedIntent)
                assertEquals(
                    "Должен открыться MainMenuActivity",
                    MainMenuActivity::class.java.name,
                    startedIntent.component?.className
                )
            }
        } catch (e: Exception) {
            val shadowApplication = Shadows.shadowOf(
                ApplicationProvider.getApplicationContext<Application>()
            )
            val startedIntent = shadowApplication.nextStartedActivity
            assertNotNull("Должен быть запущен Intent", startedIntent)
            assertEquals(
                "Должен открыться MainMenuActivity",
                MainMenuActivity::class.java.name,
                startedIntent.component?.className
            )
        }

        prefs.edit().clear().commit()
    }

    // Пользователь НЕ авторизован — переход на LoginActivity
    @Test
    fun mainActivity_userNotLoggedIn_navigatesToLogin() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val shadowApplication = Shadows.shadowOf(
                    ApplicationProvider.getApplicationContext<Application>()
                )
                val startedIntent = shadowApplication.nextStartedActivity
                assertNotNull("Должен быть запущен Intent", startedIntent)
                assertEquals(
                    "Должен открыться LoginActivity",
                    LoginActivity::class.java.name,
                    startedIntent.component?.className
                )
            }
        } catch (e: Exception) {
            val shadowApplication = Shadows.shadowOf(
                ApplicationProvider.getApplicationContext<Application>()
            )
            val startedIntent = shadowApplication.nextStartedActivity
            assertNotNull("Должен быть запущен Intent", startedIntent)
            assertEquals(
                "Должен открыться LoginActivity",
                LoginActivity::class.java.name,
                startedIntent.component?.className
            )
        }
    }

    // user_id = -1 (значение по умолчанию) — переход на LoginActivity
    @Test
    fun mainActivity_defaultUserId_navigatesToLogin() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        prefs.edit().putInt("user_id", -1).commit()

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val shadowApplication = Shadows.shadowOf(
                    ApplicationProvider.getApplicationContext<Application>()
                )
                val startedIntent = shadowApplication.nextStartedActivity
                assertNotNull("Должен быть запущен Intent", startedIntent)
                assertEquals(
                    "Должен открыться LoginActivity",
                    LoginActivity::class.java.name,
                    startedIntent.component?.className
                )
            }
        } catch (e: Exception) {
            val shadowApplication = Shadows.shadowOf(
                ApplicationProvider.getApplicationContext<Application>()
            )
            val startedIntent = shadowApplication.nextStartedActivity
            assertNotNull("Должен быть запущен Intent", startedIntent)
            assertEquals(
                "Должен открыться LoginActivity",
                LoginActivity::class.java.name,
                startedIntent.component?.className
            )
        }

        prefs.edit().clear().commit()
    }
}
