package com.example.ittermslearningapp

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.ittermslearningapp.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {

    private lateinit var appBar: LinearLayout
    private lateinit var btnBack: ImageButton
    private lateinit var tvDomain: TextView
    private lateinit var tvQuestionCounter: TextView
    private lateinit var answersContainer: LinearLayout
    private lateinit var btnSubmitAnswer: Button
    private lateinit var btnNext: Button
    private lateinit var btnPrevious: Button
    private lateinit var btnFinishTest: Button
    private lateinit var cardTermContainer: LinearLayout
    private lateinit var tvTerm: TextView
    private lateinit var navigationContainer: LinearLayout
    private lateinit var binding: ActivityTestBinding

    private val dbHelper by lazy { DatabaseHelper(this) }

    private var currentQuestionIndex = 0
    private val questionList = mutableListOf<Question>()
    private val userAnswers = mutableMapOf<Int, Int>() // индекс вопроса -> выбранный вариант

    private var currentTopicId: Int = 1
    private var currentLevel: Int = 1
    private var totalQuestions = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appBar            = findViewById(R.id.appBarTest)
        btnBack           = findViewById(R.id.btnBackTest)
        tvDomain          = findViewById(R.id.tvDomainTest)
        tvQuestionCounter = findViewById(R.id.tvQuestionCounter)
        answersContainer  = findViewById(R.id.answersContainer)
        btnSubmitAnswer   = findViewById(R.id.btnSubmitAnswer)
        btnNext           = findViewById(R.id.btnNext)
        btnFinishTest     = findViewById(R.id.btnFinishTest)
        btnPrevious       = findViewById(R.id.btnPrevious)
        cardTermContainer = findViewById(R.id.cardTermContainer)
        tvTerm            = findViewById(R.id.tvTermTest)
        navigationContainer = findViewById(R.id.navigationContainerTest)

        // topic_id и level приходят из MainMenuActivity
        currentTopicId = intent.getIntExtra("topic_id", 1)
        currentLevel   = intent.getIntExtra("level", 1)

        // Загружаем название темы из БД
        val topicName = dbHelper.getTopics()
            .find { it.id == currentTopicId }?.name ?: "Тема $currentTopicId"
        tvDomain.text = topicName

        btnBack.setOnClickListener { showExitDialog() }
        onBackPressedDispatcher.addCallback(this) { showExitDialog() }

        loadQuestions()

        if (questionList.isEmpty()) {
            Toast.makeText(this, "Нет терминов для выбранной темы и уровня", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        displayQuestion(currentQuestionIndex)

        btnSubmitAnswer.setOnClickListener {
            val selectedIndex = getSelectedAnswerIndex()
            if (selectedIndex == -1) {
                Toast.makeText(this, "Выберите вариант ответа", Toast.LENGTH_SHORT).show()
            } else {
                lockAnswers(selectedIndex)
            }
        }

        btnNext.setOnClickListener {
            if (currentQuestionIndex < questionList.size - 1) {
                currentQuestionIndex++
                displayQuestion(currentQuestionIndex)
            }
        }

        btnPrevious.setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                displayQuestion(currentQuestionIndex)
            }
        }
    }

    // Загрузка вопросов
    private fun loadQuestions() {
        // getConceptsForTest возвращает ровно limit (10) случайных терминов
        val concepts = dbHelper.getConceptsForTest(
            topicId = currentTopicId,
            level   = currentLevel,
            limit   = 10
        )

        if (concepts.isEmpty()) return

        // Все концепты для тех же темы и уровня — для генерации неправильных вариантов
        val allConcepts = dbHelper.getConceptsByLevelAndTopic(currentLevel, currentTopicId)

        totalQuestions = concepts.size
        questionList.clear()

        for (concept in concepts) {
            val wrongOptions = allConcepts
                .filter { it.id != concept.id }
                .shuffled()
                .take(3)
                .map { it.definition }

            val answers = (wrongOptions + concept.definition).shuffled()

            questionList.add(
                Question(
                    conceptId     = concept.id,
                    term          = concept.term,
                    correctAnswer = concept.definition,
                    answers       = answers
                )
            )
        }
    }

    // Отображение вопроса
    private fun displayQuestion(index: Int) {
        val question = questionList[index]
        tvTerm.text = question.term
        tvQuestionCounter.text = "${index + 1}/$totalQuestions"

        answersContainer.removeAllViews()
        btnSubmitAnswer.isEnabled = true
        btnSubmitAnswer.alpha = 1f

        question.answers.forEachIndexed { i, answerText ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.bg_card)
                setPadding(24, 24, 24, 24)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 8, 0, 8) }
            }

            val radio = RadioButton(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                buttonTintList = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                    intArrayOf(Color.parseColor("#4CAF50"), Color.BLACK)
                )
            }
            card.addView(radio)

            val tv = TextView(this, null, 0, R.style.CardDefinitionText).apply {
                text = answerText
                setPadding(16, 0, 0, 20)
            }
            card.addView(tv)

            card.setOnClickListener { if (btnSubmitAnswer.isEnabled) selectCard(card, radio) }
            radio.setOnClickListener { if (btnSubmitAnswer.isEnabled) selectCard(card, radio) }

            answersContainer.addView(card)
        }

        if (userAnswers.containsKey(index)) {
            lockAnswers(userAnswers[index]!!)
        }

        btnPrevious.visibility = if (index == 0) View.GONE else View.VISIBLE
        btnNext.visibility     = if (index == questionList.lastIndex) View.GONE else View.VISIBLE

        if (index == questionList.lastIndex) {
            btnFinishTest.visibility = View.VISIBLE
            btnNext.visibility = View.GONE
        } else {
            btnFinishTest.visibility = View.GONE
        }

        btnFinishTest.setOnClickListener {
            val skipped = getSkippedQuestionsCount()
            if (skipped == 0) showResults() else showFinishConfirmationDialog(skipped)
        }
    }

    // Выбор ответа
    private fun selectCard(card: LinearLayout, radio: RadioButton) {
        if (!btnSubmitAnswer.isEnabled) return
        for (j in 0 until answersContainer.childCount) {
            val otherCard  = answersContainer.getChildAt(j) as LinearLayout
            val otherRadio = otherCard.getChildAt(0) as RadioButton
            otherRadio.isChecked = false
            otherCard.isSelected = false
        }
        radio.isChecked = true
        card.isSelected = true
    }

    private fun getSelectedAnswerIndex(): Int {
        for (i in 0 until answersContainer.childCount) {
            if (answersContainer.getChildAt(i).isSelected) return i
        }
        return -1
    }

    private fun lockAnswers(selectedIndex: Int) {
        val question = questionList[currentQuestionIndex]
        userAnswers[currentQuestionIndex] = selectedIndex

        for (i in 0 until answersContainer.childCount) {
            val card  = answersContainer.getChildAt(i) as LinearLayout
            val radio = card.getChildAt(0) as RadioButton

            radio.isChecked  = (i == selectedIndex)
            card.isSelected  = (i == selectedIndex)
            radio.isEnabled  = false
            card.isClickable = false
            card.isEnabled   = false

            val isCorrect = question.answers[i] == question.correctAnswer
            card.isActivated = isCorrect   // true -> зелёный, false -> красный

            radio.buttonTintList = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                intArrayOf(
                    when {
                        i == selectedIndex && !isCorrect -> 0xFF7a2020.toInt()
                        i == selectedIndex && isCorrect  -> 0xFF4CAF50.toInt()
                        else                             -> 0xFF000000.toInt()
                    },
                    0xFF000000.toInt()
                )
            )
        }

        btnSubmitAnswer.isEnabled = false
        btnSubmitAnswer.alpha = 0.5f
        btnNext.isEnabled = true

        val isCorrect = question.answers[selectedIndex] == question.correctAnswer
        updateUserProgressAfterAnswer(currentQuestionIndex, isCorrect)
    }

    private fun updateUserProgressAfterAnswer(questionIndex: Int, isCorrect: Boolean) {
        val prefs  = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)
        if (userId == -1) return

        val status = if (isCorrect) "Знаю" else "Повторяю"
        dbHelper.upsertProgressAfterTest(userId, questionList[questionIndex].conceptId, status)
    }

    // Диалог выхода
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

        // иконка-предупреждение
        root.addView(TextView(this).apply {
            text = emoji
            textSize = 36f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * dp).toInt() }
        })

        // заголовок
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

        // сообщение
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

        // разделитель
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

    // Для выхода из теста
    private fun showExitDialog() {
        showExitConfirmation(
            modeName = "теста",
            emoji = "📝"
        ) {
            showResults()
        }
    }

    // Окно подтверждения завершения теста
    private fun showFinishConfirmationDialog(skippedCount: Int) {
        val dp = resources.displayMetrics.density

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * dp).toInt(), (20 * dp).toInt(), (24 * dp).toInt(), (12 * dp).toInt())
        }

        root.addView(TextView(this).apply {
            text = "⚠️"
            textSize = 36f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * dp).toInt() }
        })

        root.addView(TextView(this).apply {
            text = "Тест не завершён"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_primary))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * dp).toInt() }
        })

        val message = SpannableStringBuilder()
            .append("Вы не ответили на ")
            .append(boldColored("$skippedCount", Color.parseColor("#D32F2F"), 18f, this))
            .append(" ${wordForm(skippedCount)}.\n\nХотите завершить тест?")

        root.addView(TextView(this).apply {
            text = message
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_body))
            // Чуть больше междустрочного интервала
            setLineSpacing(4 * dp, 1f)
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
            .setPositiveButton("Завершить") { _, _ -> showResults() }
            .setNegativeButton("Продолжить", null)
            .setCancelable(false)
            .show()
    }

    private fun boldColored(text: String, color: Int, sizeSp: Float, context: Context): SpannableString {
        return SpannableString(text).apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(ForegroundColorSpan(color), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(sizeSp.toSp(context).toInt()), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun Float.toSp(context: Context) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, this, context.resources.displayMetrics
    )

    private fun getSkippedQuestionsCount() = questionList.size - userAnswers.size

    private fun wordForm(count: Int): String = when {
        count % 100 in 11..14 -> "вопросов"
        count % 10 == 1       -> "вопрос"
        count % 10 in 2..4    -> "вопроса"
        else                  -> "вопросов"
    }

    // Окно результатов

    private fun showResults() {
        val correctCount = userAnswers.count { (index, answer) ->
            questionList[index].answers[answer] == questionList[index].correctAnswer
        }
        val wrongTerms = userAnswers
            .filter { (index, answer) ->
                questionList[index].answers[answer] != questionList[index].correctAnswer
            }
            .map { questionList[it.key].term }

        val total    = questionList.size
        val percent  = (correctCount * 100.0 / total).toInt()
        val emoji    = when {
            percent == 100 -> "🏆"
            percent >= 70  -> "👍"
            percent >= 40  -> "📖"
            else           -> "💪"
        }
        val grade = when {
            percent == 100 -> "Отлично!"
            percent >= 70  -> "Хороший результат"
            percent >= 40  -> "Нужно повторить"
            else           -> "Продолжай учиться"
        }

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

        root.addView(TextView(this).apply {
            text = "$emoji  $percent%"
            textSize = 40f
            setTypeface(null, Typeface.BOLD)
            setTextColor(scoreColor(percent))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (4 * dp).toInt() }
        })

        // Оценка
        root.addView(TextView(this).apply {
            text = grade
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.dialog_text_secondary))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * dp).toInt() }
        })

        // Счёт
        root.addView(resultRow("Правильных ответов:", "$correctCount из $total", scoreColor(percent), dp))

        // Разделитель
        root.addView(divider(dp))

        // Ошибки
        if (wrongTerms.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "Ошибок нет — все термины изучены верно!"
                textSize = 14f
                setTextColor(Color.parseColor("#2E7D32"))
                setPadding(0, (8 * dp).toInt(), 0, 0)
            })
        } else {
            root.addView(TextView(this).apply {
                text = "Термины с ошибками:"
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#B71C1C"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (8 * dp).toInt(); bottomMargin = (4 * dp).toInt() }
            })

            wrongTerms.forEach { term ->
                root.addView(TextView(this).apply {
                    text = "• $term"
                    textSize = 14f
                    setTextColor(ContextCompat.getColor(context, R.color.dialog_text_body))
                    setPadding((8 * dp).toInt(), (2 * dp).toInt(), 0, (2 * dp).toInt())
                })
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Результаты теста")
            .setView(scroll)
            .setPositiveButton("В главное меню") { _, _ ->
                startActivity(
                    Intent(this, MainMenuActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                )
                finish()
            }
            .setCancelable(false)
            .show()
    }

    // Вспомогательные методы для построения результатов

    private fun scoreColor(percent: Int): Int = when {
        percent >= 70 -> Color.parseColor("#2E7D32")
        percent >= 40 -> Color.parseColor("#E65100")
        else          -> Color.parseColor("#B71C1C")
    }

    private fun resultRow(label: String, value: String, valueColor: Int, dp: Float): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (6 * dp).toInt() }

            addView(TextView(this@TestActivity).apply {
                text = label
                textSize = 14f
                setTextColor(Color.parseColor("#666666"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@TestActivity).apply {
                text = value
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
                setTextColor(valueColor)
            })
        }
    }

    private fun divider(dp: Float): View {
        return View(this).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.dialog_divider))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * dp).toInt()
            ).apply {
                topMargin    = (12 * dp).toInt()
                bottomMargin = (12 * dp).toInt()
            }
        }
    }

    // Класс "Вопрос"

    data class Question(
        val conceptId: Int,
        val term: String,
        val correctAnswer: String,
        val answers: List<String>
    )
}