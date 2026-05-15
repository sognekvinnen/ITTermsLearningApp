package com.example.ittermslearningapp

import android.content.Intent
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class LoginActivityTest {

    // Кнопки видны при запуске
    @Test
    fun loginActivity_onLaunch_bothButtonsAreVisible() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val btnAuth = activity.findViewById<Button>(R.id.btnAuthorisation)
                val btnReg = activity.findViewById<Button>(R.id.btnRegistration)

                assertEquals(android.view.View.VISIBLE, btnAuth.visibility)
                assertEquals(android.view.View.VISIBLE, btnReg.visibility)
            }
        }
    }

    // Кнопка "Авторизация" открывает AuthorisationActivity
    @Test
    fun loginActivity_clickBtnAuthorisation_startsAuthorisationActivity() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnAuthorisation).performClick()

                val started = shadowOf(activity).nextStartedActivity
                assertEquals(
                    "Должна открыться AuthorisationActivity",
                    AuthorisationActivity::class.java.name,
                    started?.component?.className
                )
            }
        }
    }

    // Кнопка "Регистрация" открывает RegistrationActivity
    @Test
    fun loginActivity_clickBtnRegistration_startsRegistrationActivity() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnRegistration).performClick()

                val started = shadowOf(activity).nextStartedActivity
                assertEquals(
                    "Должна открыться RegistrationActivity",
                    RegistrationActivity::class.java.name,
                    started?.component?.className
                )
            }
        }
    }

    // Кнопка "Авторизация" НЕ открывает RegistrationActivity
    @Test
    fun loginActivity_clickBtnAuthorisation_doesNotStartRegistrationActivity() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnAuthorisation).performClick()

                val started = shadowOf(activity).nextStartedActivity
                assertNotEquals(
                    "Кнопка авторизации не должна открывать RegistrationActivity",
                    RegistrationActivity::class.java.name,
                    started?.component?.className
                )
            }
        }
    }

    // Кнопка "Регистрация" НЕ открывает AuthorisationActivity
    @Test
    fun loginActivity_clickBtnRegistration_doesNotStartAuthorisationActivity() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnRegistration).performClick()

                val started = shadowOf(activity).nextStartedActivity
                assertNotEquals(
                    "Кнопка регистрации не должна открывать AuthorisationActivity",
                    AuthorisationActivity::class.java.name,
                    started?.component?.className
                )
            }
        }
    }
}