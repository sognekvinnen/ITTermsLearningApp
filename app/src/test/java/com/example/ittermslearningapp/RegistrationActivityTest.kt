package com.example.ittermslearningapp

import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class RegistrationActivityTest {

    // Проверка минимальной длины логина (4 символа)
    @Test
    fun registration_usernameExactly4Chars_passesLengthCheck() {
        ActivityScenario.launch(RegistrationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etUsername).setText("user")
                activity.findViewById<EditText>(R.id.etPassword).setText("pass12")
                activity.findViewById<EditText>(R.id.etPasswordRepeat).setText("pass12")
                activity.findViewById<Button>(R.id.btnRegister).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvRegError)
                assertFalse(
                    "Не должно быть ошибки о минимуме 4 символов для логина",
                    tvError.text.contains("минимум 4")
                )
            }
        }
    }

    // Проверка минимальной длины пароля (5 символов)
    @Test
    fun registration_passwordExactly5Chars_passesLengthCheck() {
        ActivityScenario.launch(RegistrationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etUsername).setText("user2")
                activity.findViewById<EditText>(R.id.etPassword).setText("abcde")
                activity.findViewById<EditText>(R.id.etPasswordRepeat).setText("abcde")
                activity.findViewById<Button>(R.id.btnRegister).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvRegError)
                assertFalse(
                    "Не должно быть ошибки о минимуме 5 символов для пароля",
                    tvError.text.contains("минимум 5")
                )
            }
        }
    }

    // Проверка хеширования пароля
    @Test
    fun hashPassword_viaReflection_returns64CharHex() {
        ActivityScenario.launch(RegistrationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val method = RegistrationActivity::class.java
                    .getDeclaredMethod("hashPassword", String::class.java)
                    .apply { isAccessible = true }

                val result = method.invoke(activity, "test123") as String
                assertEquals("Хеш должен быть 64 символа", 64, result.length)
                assertTrue("Хеш должен состоять из hex-символов", result.matches(Regex("[0-9a-f]+")))
            }
        }
    }

    // Ошибка пустых полей
    @Test
    fun registration_allFieldsEmpty_showsFillAllFieldsError() {
        ActivityScenario.launch(RegistrationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.btnRegister).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvRegError)
                assertEquals(android.view.View.VISIBLE, tvError.visibility)
                assertEquals("Заполните все поля", tvError.text.toString())
            }
        }
    }

    // Ошибка короткого логина
    @Test
    fun registration_usernameTooShort_showsMinLengthError() {
        ActivityScenario.launch(RegistrationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etUsername).setText("abc")
                activity.findViewById<EditText>(R.id.etPassword).setText("password1")
                activity.findViewById<EditText>(R.id.etPasswordRepeat).setText("password1")
                activity.findViewById<Button>(R.id.btnRegister).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvRegError)
                assertEquals(android.view.View.VISIBLE, tvError.visibility)
                assertTrue(
                    "Сообщение об ошибке должно упоминать минимум 4 символа",
                    tvError.text.contains("4")
                )
            }
        }
    }

    // Ошибка короткого пароля
    @Test
    fun registration_passwordTooShort_showsPasswordLengthError() {
        ActivityScenario.launch(RegistrationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etUsername).setText("validuser")
                activity.findViewById<EditText>(R.id.etPassword).setText("abc")
                activity.findViewById<EditText>(R.id.etPasswordRepeat).setText("abc")
                activity.findViewById<Button>(R.id.btnRegister).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvRegError)
                assertEquals(android.view.View.VISIBLE, tvError.visibility)
                assertTrue(
                    "Сообщение об ошибке должно упоминать минимум 5 символов",
                    tvError.text.contains("5")
                )
            }
        }
    }

    // Ошибка несовпадения паролей
    @Test
    fun registration_passwordMismatch_showsMismatchError() {
        ActivityScenario.launch(RegistrationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etUsername).setText("validuser")
                activity.findViewById<EditText>(R.id.etPassword).setText("password1")
                activity.findViewById<EditText>(R.id.etPasswordRepeat).setText("password2")
                activity.findViewById<Button>(R.id.btnRegister).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvRegError)
                assertEquals(android.view.View.VISIBLE, tvError.visibility)
                assertEquals("Пароли не совпадают", tvError.text.toString())
            }
        }
    }
}