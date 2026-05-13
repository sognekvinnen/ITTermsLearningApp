package com.example.ittermslearningapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ittermslearningapp.databinding.ActivityMainMenuBinding
import androidx.core.graphics.toColorInt

class MainMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMenuBinding
    private lateinit var db: DatabaseHelper

    private val topics = listOf(
        1 to "Аппаратное обеспечение",
        2 to "Программное обеспечение",
        3 to "Компьютерные сети",
        4 to "Данные",
        5 to "Разработка",
        6 to "Безопасность"
    )

    private val levels = listOf(
        1 to "Простой",
        2 to "Средний",
        3 to "Сложный"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = DatabaseHelper(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left, systemBars.top,
                systemBars.right, systemBars.bottom
            )
            insets
        }

        // Читаем имя пользователя из сессии SharedPreferences
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        val username = prefs.getString("username", "Пользователь") ?: "Пользователь"

        val appName = "IT Atlas"
        val fullText = "Добро пожаловать в $appName, $username!"
        val spannable = SpannableString(fullText)

        val appStart = fullText.indexOf(appName)
        spannable.setSpan(StyleSpan(Typeface.BOLD), appStart, appStart + appName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#B00020")), appStart, appStart + appName.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val nameStart = fullText.lastIndexOf(username)
        spannable.setSpan(StyleSpan(Typeface.BOLD), nameStart, nameStart + username.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannable.setSpan(ForegroundColorSpan(Color.parseColor("#B00020")), nameStart, nameStart + username.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvMainMenuWelcome.text = spannable

        // Кнопка "Обучение" — выбор темы -> выбор уровня -> переход в LearningActivity
        binding.btnLearning.setOnClickListener {
            showTopicDialog { topicId ->
                showLevelDialog { level ->
                    val intent = Intent(this, LearningActivity::class.java)
                    intent.putExtra("topic_id", topicId)
                    intent.putExtra("level", level)
                    startActivity(intent)
                }
            }
        }

        // Кнопка "Тест" — выбор темы -> выбор уровня -> переход в TestActivity
        binding.btnTest.setOnClickListener {
            showTopicDialog { topicId ->
                showLevelDialog { level ->
                    val intent = Intent(this, TestActivity::class.java)
                    intent.putExtra("topic_id", topicId)
                    intent.putExtra("level", level)
                    startActivity(intent)
                }
            }
        }

        // Кнопка "Мой прогресс"
        binding.btnProgress.setOnClickListener {
            startActivity(Intent(this, MyProgressActivity::class.java))
        }

        // Кнопка "Выйти из аккаунта"
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        // Системная кнопка "Назад" — сворачиваем приложение
        onBackPressedDispatcher.addCallback(this) {
            moveTaskToBack(true)
        }
    }

    // Диалог выбора темы
    private fun showTopicDialog(onSelected: (topicId: Int) -> Unit) {
        val dp = resources.displayMetrics.density

        val scroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
        }
        scroll.addView(root)

        // Иконка
        root.addView(TextView(this).apply {
            text = "📚"
            textSize = 36f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (4 * dp).toInt() }
        })

        // Подзаголовок
        root.addView(TextView(this).apply {
            text = "Выберите тему"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_primary))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * dp).toInt() }
        })

        // Разделитель
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).apply { bottomMargin = (8 * dp).toInt() }
            setBackgroundColor(Color.parseColor("#E0E0E0"))
        })

        var dialog: AlertDialog? = null

        topics.forEachIndexed { index, (topicId, topicName) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(
                    (8 * dp).toInt(), (10 * dp).toInt(),
                    (8 * dp).toInt(), (10 * dp).toInt()
                )
                isClickable = true
                isFocusable = true
                background = with(android.util.TypedValue()) {
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
                    resources.getDrawable(resourceId, theme)
                }
            }

            // Номер темы
            row.addView(TextView(this).apply {
                text = "${index + 1}."
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.dialog_text_hint))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (10 * dp).toInt() }
            })

            // Название темы
            row.addView(TextView(this).apply {
                text = topicName
                textSize = 15f
                setTextColor(ContextCompat.getColor(context, R.color.dialog_text_item))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            // Стрелочка
            row.addView(TextView(this).apply {
                text = "›"
                textSize = 20f
                setTextColor(ContextCompat.getColor(context, R.color.dialog_text_arrow))
            })

            row.setOnClickListener {
                dialog?.dismiss()
                onSelected(topicId)
            }
            root.addView(row)

            // Разделитель между строками (кроме последней)
            if (index < topics.lastIndex) {
                root.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
                    ).apply {
                        marginStart = (28 * dp).toInt()
                    }
                    setBackgroundColor("#E0E0E0".toColorInt())
                })
            }
        }

        dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .setCancelable(true)
            .create()
        dialog.show()
    }

    // Диалог выбора уровня
    private fun showLevelDialog(onSelected: (level: Int) -> Unit) {
        val dp = resources.displayMetrics.density

        val levelAccents = listOf(
            "#4CAF50" to "Простой",
            "#FF9800" to "Средний",
            "#F44336" to "Сложный"
        )

        val scroll = ScrollView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (16 * dp).toInt(), (20 * dp).toInt(), (8 * dp).toInt())
        }
        scroll.addView(root)

        root.addView(TextView(this).apply {
            text = "🎯"
            textSize = 36f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (4 * dp).toInt() }
        })

        root.addView(TextView(this).apply {
            text = "Выберите уровень"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_primary))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * dp).toInt() }
        })

        // Разделитель
        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).apply { bottomMargin = (8 * dp).toInt() }
            setBackgroundColor("#E0E0E0".toColorInt())
        })

        var dialog: AlertDialog? = null

        levels.forEachIndexed { index, (level, _) ->
            val (accentColor, levelName) = levelAccents.getOrElse(index) { "#607D8B" to "Уровень" }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding((8 * dp).toInt(), (12 * dp).toInt(), (8 * dp).toInt(), (12 * dp).toInt())
                isClickable = true
                isFocusable = true
                background = with(android.util.TypedValue()) {
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
                    resources.getDrawable(resourceId, theme)
                }
            }

            row.addView(View(this).apply {
                // Устанавливаем размер круга (20dp)
                val size = (20 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = (12 * dp).toInt()
                }

                // Создаем круглую форму программно
                val shape = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(accentColor.toColorInt())
                }
                background = shape
            })

            // Название уровня
            row.addView(TextView(this).apply {
                text = levelName
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(accentColor.toColorInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            // Стрелочка вправо
            row.addView(TextView(this).apply {
                text = "›"
                textSize = 20f
                setTextColor(ContextCompat.getColor(context, R.color.dialog_text_arrow))
            })

            row.setOnClickListener {
                dialog?.dismiss()
                onSelected(level)
            }
            root.addView(row)

            // разделитель между строками
            if (index < levels.lastIndex) {
                root.addView(View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
                    ).apply { marginStart = (42 * dp).toInt() }
                    setBackgroundColor("#F0F0F0".toColorInt())
                })
            }
        }

        dialog = AlertDialog.Builder(this)
            .setView(scroll)
            .setCancelable(true)
            .create()
        dialog.show()
    }

    // Выход
    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Выход из аккаунта")
            .setMessage("Вы уверены, что хотите выйти из аккаунта?")
            .setPositiveButton("Да") { _, _ -> logout() }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun logout() {
        getSharedPreferences("user_session", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}