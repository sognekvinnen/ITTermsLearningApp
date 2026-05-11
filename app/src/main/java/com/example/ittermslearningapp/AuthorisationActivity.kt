package com.example.ittermslearningapp

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ittermslearningapp.databinding.ActivityAuthorisationBinding
import com.example.ittermslearningapp.DatabaseHelper
import java.security.MessageDigest

class AuthorisationActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var btnBack: ImageButton
    private lateinit var binding: ActivityAuthorisationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthorisationBinding.inflate(layoutInflater)
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


        btnBack = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { showExitDialog() }

        dbHelper = DatabaseHelper(this)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        val usernameFilter = InputFilter { source, _, _, _, _, _ ->
            val regex = Regex("[a-zA-Z0-9._]+")
            if (source.isEmpty() || source.matches(regex)) source else ""
        }

        etUsername.filters = arrayOf(usernameFilter, InputFilter.LengthFilter(20))

        btnConfirm.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val passwordHash = hashPassword(password)

            if (username.isEmpty() || password.isEmpty()) {
                showError("Введите логин и пароль")
                return@setOnClickListener
            }

            val user = dbHelper.getUserByUsername(username)

            if (user == null) {
                showError("Пользователь с таким логином не найден")
                return@setOnClickListener
            }

            if (user.password != passwordHash) {
                showError("Неверный пароль, попробуйте ещё раз")
                return@setOnClickListener
            }

            // Успешная авторизация
            val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
            prefs.edit()
                .putInt("user_id", user.id)
                .putString("username", user.username)
                .apply()

            startActivity(Intent(this, MainMenuActivity::class.java))
            finish()
        }
    }

    private fun showError(message: String) {
        val errorView = findViewById<TextView>(R.id.tvAuthError)
        errorView.text = message
        errorView.visibility = View.VISIBLE
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