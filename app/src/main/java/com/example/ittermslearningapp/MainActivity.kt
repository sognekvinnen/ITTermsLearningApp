package com.example.ittermslearningapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        val nextActivity = if (userId != -1) {
            // Пользователь уже авторизован
            Intent(this, MainMenuActivity::class.java)
        } else {
            // Пользователь ещё НЕ авторизован
            Intent(this, LoginActivity::class.java)
        }

        startActivity(nextActivity)
        finish()
    }
}