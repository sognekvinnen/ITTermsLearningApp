package com.example.ittermslearningapp

import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ittermslearningapp.databinding.ActivityRegistrationBinding
import java.security.MessageDigest

class RegistrationActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var btnBack: ImageButton
    private lateinit var binding: ActivityRegistrationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        dbHelper = DatabaseHelper(this)

        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { showExitDialog() }

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPasswordRepeat = findViewById<EditText>(R.id.etPasswordRepeat)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvError = findViewById<TextView>(R.id.tvRegError)

        val usernameFilter = InputFilter { source, _, _, _, _, _ ->
            val regex = Regex("[a-zA-Z0-9._]+")
            if (source.matches(regex)) source else ""
        }

        etUsername.filters = arrayOf(
            usernameFilter,
            InputFilter.LengthFilter(20)
        )

        val passwordFilter = InputFilter { source, _, _, _, _, _ ->
            if (source.contains(" ") || source.contains("\t")) "" else source
        }

        etPassword.filters = arrayOf(passwordFilter)
        etPasswordRepeat.filters = arrayOf(passwordFilter)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val passwordRepeat = etPasswordRepeat.text.toString().trim()
            val passwordHash = hashPassword(password)

            if (username.isEmpty() || password.isEmpty() || passwordRepeat.isEmpty()) {
                showError(tvError, "Заполните все поля")
                return@setOnClickListener
            }

            if (username.length < 4) {
                showError(tvError, "Логин должен содержать минимум 4 символа")
                return@setOnClickListener
            }

            if (dbHelper.getUserByUsername(username) != null) {
                showError(tvError, "Пользователь с таким логином уже существует")
                return@setOnClickListener
            }

            if (password.length < 5) {
                showError(tvError, "Пароль должен содержать минимум 5 символов")
                return@setOnClickListener
            }

            if (password != passwordRepeat) {
                showError(tvError, "Пароли не совпадают")
                return@setOnClickListener
            }

            try {
                // запись нового пользователя в БД
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("username", username)
                    put("password_hash", passwordHash)
                }
                db.insertOrThrow("User", null, values)
                db.close()

                // Проверка на добавление
                val newUser = dbHelper.getUserByUsername(username)
                if (newUser != null) {
                    Toast.makeText(this, "Регистрация успешна!", Toast.LENGTH_SHORT).show()

                    // SharedPreferences
                    val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
                    prefs.edit()
                        .putInt("user_id", newUser.id)
                        .putString("username", newUser.username)
                        .apply()

                    // Переход в MainMenuActivity
                    startActivity(Intent(this, MainMenuActivity::class.java))
                    finish()
                } else {
                    showError(tvError, "Ошибка при регистрации. Попробуйте снова")
                }

            } catch (e: Exception) {
                // Если username уже существует или другая ошибка
                showError(tvError, "Ошибка: ${e.message}")
            }
        }
    }

    private fun showError(tv: TextView, message: String) {
        tv.text = message
        tv.visibility = View.VISIBLE
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти на экран аутентификации?")
            .setPositiveButton("Да") { _, _ ->
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }
}