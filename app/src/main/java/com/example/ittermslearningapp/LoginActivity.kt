package com.example.ittermslearningapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ittermslearningapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    // Создаём binding переменную
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
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

        // Кнопка "Авторизация"
        binding.btnAuthorisation.setOnClickListener {
            val intent = Intent(this, AuthorisationActivity::class.java) // заглушка
            startActivity(intent)
        }

        // Кнопка "Регистрация"
        binding.btnRegistration.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java) // заглушка
            startActivity(intent)
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти из приложения?")
            .setPositiveButton("Да") { _, _ -> finishAffinity() }
            .setNegativeButton("Нет", null)
            .show()
    }
}