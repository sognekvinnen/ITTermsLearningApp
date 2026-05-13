package com.example.ittermslearningapp

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ittermslearningapp.DatabaseHelper.Concept
import com.example.ittermslearningapp.databinding.ActivityLearningBinding

class LearningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLearningBinding
    private lateinit var dbHelper: DatabaseHelper

    private val viewedConcepts = mutableMapOf<Int, Int>() // conceptId -> количество просмотров

    private var currentConcept: Concept? = null
    private var isDefinitionVisible = true

    private var currentTopicId: Int = 1
    private var currentLevel: Int = 1
    private var userId: Int = -1

    // Карта id темы -> название темы, загружается один раз при старте
    private var topicNamesMap: Map<Int, String> = emptyMap()

    // НАВИГАЦИЯ
    private val backStack = mutableListOf<Concept>()
    private val forwardStack = mutableListOf<Concept>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLearningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                systemBars.left, systemBars.top,
                systemBars.right, systemBars.bottom
            )
            insets
        }

        dbHelper = DatabaseHelper(this)

        // topic_id и level приходят из Intent от MainMenuActivity
        currentTopicId = intent.getIntExtra("topic_id", 1)
        currentLevel   = intent.getIntExtra("level", 1)

        topicNamesMap = dbHelper.getTopics().associate { it.id to it.name }

        initUserSession()

        binding.btnBack.setOnClickListener { showExitDialog() }
        onBackPressedDispatcher.addCallback(this) { showExitDialog() }

        binding.btnToggleDefinition.setOnClickListener { toggleDefinition() }
        binding.btnPrevious.setOnClickListener { goBack() }
        binding.btnNext.setOnClickListener { goForward() }

        loadInitialTerm()
    }

    private fun initUserSession() {
        val prefs = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("user_id", -1)
        if (userId == -1) {
            Toast.makeText(this, "Ошибка пользователя. Перезапустите приложение.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun loadInitialTerm() {
        val concept = dbHelper.getRandomConcept(currentLevel, currentTopicId)
        if (concept != null) {
            displayConcept(concept, saveHistory = false)
        } else {
            Toast.makeText(this, "Термины не найдены", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayConcept(concept: Concept, saveHistory: Boolean = true) {
        if (saveHistory && currentConcept != null) {
            backStack.add(currentConcept!!)
            forwardStack.clear()
        }

        currentConcept = concept
        isDefinitionVisible = true
        registerConceptView(concept.id)

        binding.tvTerm.text = concept.term
        binding.tvDefinition.text = concept.definition
        binding.tvLevel.text = difficultyLabel(concept.difficulty)
        binding.btnToggleDefinition.text = "Спрятать определение"

        // Тема в AppBar всегда соответствует теме текущего термина
        binding.tvDomain.text = topicNamesMap[concept.topicId] ?: "Тема ${concept.topicId}"

        updateNavigationButtons()
        displayRelatedCategories(concept)
    }

    private fun displayRelatedCategories(concept: Concept) {
        val container = binding.relatedContainer
        container.removeAllViews()

        val relatedMap = dbHelper.getRelatedConcepts(concept.id)

        for ((categoryName, list) in relatedMap) {
            if (list.isEmpty()) continue

            val sorted = sortRelatedConcepts(list, concept.topicId, concept.difficulty)

            val title = TextView(this).apply {
                text = categoryName
                textSize = 16f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.typeofconnection))
                setPadding(0, 16, 0, 8)
            }
            container.addView(title)

            sorted.forEach { (relatedConcept, labelPrefix) ->
                val levelStr = difficultyLabel(relatedConcept.difficulty)
                val displayText = if (labelPrefix != null) {
                    "$labelPrefix\n${relatedConcept.term} ($levelStr)"
                } else {
                    "${relatedConcept.term} ($levelStr)"
                }

                val termView = TextView(this).apply {
                    text = displayText
                    textSize = 14f
                    setTextColor(
                        if (labelPrefix != null)
                            Color.parseColor("#888888")  // другая тема — приглушённый цвет
                        else
                            Color.parseColor("#D2691E")  // та же тема — акцентный цвет
                    )
                    setPadding(16, 4, 0, 8)
                    setOnClickListener { displayConcept(relatedConcept) }
                }
                container.addView(termView)
            }
        }
    }

    private fun sortRelatedConcepts(
        list: List<Concept>,
        conceptTopicId: Int,
        conceptLevel: Int
    ): List<Pair<Concept, String?>> {
        val sameTopic  = list.filter { it.topicId == conceptTopicId }
        val otherTopic = list.filter { it.topicId != conceptTopicId }

        val levelOrder = buildLevelOrder(conceptLevel)

        val sameTopicSorted = levelOrder.flatMap { lvl ->
            sameTopic.filter { it.difficulty == lvl }
        }.map { it to null as String? }

        val otherTopicSorted = otherTopic.map { c ->
            val topicName = topicNamesMap[c.topicId] ?: "Другая тема"
            c to "Из темы: $topicName"
        }

        return sameTopicSorted + otherTopicSorted
    }

    private fun buildLevelOrder(selected: Int): List<Int> {
        val result = mutableListOf(selected)
        for (lvl in selected - 1 downTo 1) result.add(lvl)
        for (lvl in selected + 1..3) result.add(lvl)
        return result
    }

    private fun difficultyLabel(difficulty: Int) = when (difficulty) {
        1    -> "Простой уровень"
        2    -> "Средний уровень"
        3    -> "Сложный уровень"
        else -> "Уровень $difficulty"
    }

    private fun goBack() {
        if (backStack.isEmpty()) return
        val previous = backStack.removeAt(backStack.lastIndex)
        currentConcept?.let { forwardStack.add(it) }
        displayConcept(previous, saveHistory = false)
    }

    private fun goForward() {
        if (forwardStack.isEmpty()) return
        val next = forwardStack.removeAt(forwardStack.lastIndex)
        currentConcept?.let { backStack.add(it) }
        displayConcept(next, saveHistory = false)
    }

    private fun updateNavigationButtons() {
        binding.btnPrevious.visibility = if (backStack.isNotEmpty()) View.VISIBLE else View.GONE
        binding.btnNext.visibility     = if (forwardStack.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun toggleDefinition() {
        currentConcept ?: return
        isDefinitionVisible = !isDefinitionVisible
        binding.tvDefinition.text =
            if (isDefinitionVisible) currentConcept!!.definition else ""
        binding.btnToggleDefinition.text =
            if (isDefinitionVisible) "Спрятать определение" else "Показать определение"
    }

    private fun showExitConfirmation(
        modeName: String,
        emoji: String,
        onConfirm: () -> Unit
    ) {
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * dp).toInt(), (20 * dp).toInt(), (24 * dp).toInt(), (12 * dp).toInt())
        }

        root.addView(TextView(this).apply {
            text = emoji
            textSize = 36f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * dp).toInt() }
        })

        root.addView(TextView(this).apply {
            text = "Выход из режима $modeName"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_primary))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * dp).toInt() }
        })

        root.addView(TextView(this).apply {
            text = "Вы уверены, что хотите выйти из режима $modeName?"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_body))
            setLineSpacing(4 * dp, 1f)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * dp).toInt() }
        })

        root.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * dp).toInt()
            ).apply {
                topMargin = (8 * dp).toInt()
                bottomMargin = (8 * dp).toInt()
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.dialog_divider))
        })

        AlertDialog.Builder(this)
            .setView(root)
            .setPositiveButton("Да") { _, _ -> onConfirm() }
            .setNegativeButton("Нет", null)
            .setCancelable(false)
            .show()
    }

    private fun showExitDialog() {
        showExitConfirmation(
            modeName = "обучения",
            emoji = "📚"
        ) {
            saveLearningProgress()
            val intent = Intent(this, MainMenuActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun registerConceptView(conceptId: Int) {
        viewedConcepts[conceptId] = (viewedConcepts[conceptId] ?: 0) + 1
    }

    private fun saveLearningProgress() {
        if (viewedConcepts.isEmpty()) return
        dbHelper.applyLearningProgressBatch(userId = userId, views = viewedConcepts)
        viewedConcepts.clear()
    }

    override fun onPause() {
        super.onPause()
        saveLearningProgress()
    }

    override fun onDestroy() {
        saveLearningProgress()
        super.onDestroy()
    }
}