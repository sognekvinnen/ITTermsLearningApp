package com.example.ittermslearningapp

import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class AuthorisationActivityTest {

    // Дублирование hashPassword из активности
    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Проверка длины хеша (64 символа)
    @Test
    fun hashPassword_anyInput_returns64CharHexString() {
        val hash = sha256("password123")
        assertEquals("SHA-256 hex должен быть 64 символа", 64, hash.length)
        assertTrue("Хеш должен содержать только hex-символы", hash.matches(Regex("[0-9a-f]+")))
    }

    // Детерминированность хеширования
    @Test
    fun hashPassword_sameInput_alwaysReturnsSameHash() {
        assertEquals(sha256("qwerty"), sha256("qwerty"))
    }

    // Чувствительность к регистру
    @Test
    fun hashPassword_differentCases_produceDifferentHashes() {
        assertNotEquals(
            "Хеш 'Password' и 'password' должны различаться",
            sha256("Password"), sha256("password")
        )
    }

    // Ошибка пустого логина
    @Test
    fun authorisation_emptyUsername_showsCredentialError() {
        ActivityScenario.launch(AuthorisationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etPassword).setText("somepass")
                activity.findViewById<Button>(R.id.btnConfirm).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvAuthError)
                assertEquals(android.view.View.VISIBLE, tvError.visibility)
                assertEquals("Введите логин и пароль", tvError.text.toString())
            }
        }
    }

    // Ошибка пустого пароля
    @Test
    fun authorisation_emptyPassword_showsError() {
        ActivityScenario.launch(AuthorisationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etUsername).setText("validuser")
                activity.findViewById<Button>(R.id.btnConfirm).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvAuthError)
                assertEquals(android.view.View.VISIBLE, tvError.visibility)
            }
        }
    }

    // Ошибка несуществующего пользователя
    @Test
    fun authorisation_unknownUser_showsNotFoundError() {
        ActivityScenario.launch(AuthorisationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etUsername).setText("__nosuchuser__xyz__")
                activity.findViewById<EditText>(R.id.etPassword).setText("irrelevant")
                activity.findViewById<Button>(R.id.btnConfirm).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvAuthError)
                assertEquals(android.view.View.VISIBLE, tvError.visibility)
                assertEquals(
                    "Пользователь с таким логином не найден",
                    tvError.text.toString()
                )
            }
        }
    }

    // Ошибка неверного пароля
    @Test
    fun authorisation_wrongPassword_showsWrongPasswordError() {
        // Прямая регистрация через БД
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = DatabaseHelper(context)
        val hashedCorrect = sha256("correctpass")
        // Поиск или вставка тестового пользователя
        val existing = db.getUserByUsername("authtestuser")
        if (existing == null) {
            db.writableDatabase.execSQL(
                "INSERT INTO User (username, password_hash) VALUES (?, ?)",
                arrayOf("authtestuser", hashedCorrect)
            )
        }
        db.close()

        ActivityScenario.launch(AuthorisationActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<EditText>(R.id.etUsername).setText("authtestuser")
                activity.findViewById<EditText>(R.id.etPassword).setText("wrongpass")
                activity.findViewById<Button>(R.id.btnConfirm).performClick()

                val tvError = activity.findViewById<TextView>(R.id.tvAuthError)
                assertEquals(android.view.View.VISIBLE, tvError.visibility)
                assertEquals("Неверный пароль, попробуйте ещё раз", tvError.text.toString())
            }
        }
    }
}