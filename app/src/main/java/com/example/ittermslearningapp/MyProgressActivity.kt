package com.example.ittermslearningapp

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ittermslearningapp.databinding.ActivityMyProgressBinding
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import android.widget.ArrayAdapter

class MyProgressActivity : AppCompatActivity() {
    enum class SortColumn { NONE, TERM, DIFFICULTY, STATUS, REPETITIONS, TOPIC }

    data class SortState(
        val column: SortColumn = SortColumn.NONE,
        val ascending: Boolean = true
    )

    // Порядок статусов
    private val STATUS_SORT_ORDER = mapOf(
        "Изучаю"    to 0,
        "Повторяю"  to 1,
        "Знаю" to 2
    )

    // Подписи заголовков таблицы
    private val HEADER_LABELS = mapOf(
        SortColumn.TERM        to "Термин",
        SortColumn.DIFFICULTY  to "Сложность",
        SortColumn.STATUS      to "Статус",
        SortColumn.REPETITIONS to "Кол-во повторений",
        SortColumn.TOPIC       to "Тема"
    )

    private lateinit var binding: ActivityMyProgressBinding
    private lateinit var recyclerView: RecyclerView
    private lateinit var dbHelper: DatabaseHelper

    private var userId: Int = -1
    private var allTopics: List<DatabaseHelper.Topic> = emptyList()

    private var selectedTopic: DatabaseHelper.Topic? = null
    private var selectedLevel: Int? = null      // null = все уровни; 1/2/3 = Простой/Средний/Сложный
    private var selectedStatus: String? = null  // null = все статусы

    private var sortState = SortState()

    // onCreate 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        recyclerView = binding.recyclerProgress
        recyclerView.layoutManager = LinearLayoutManager(this)

        dbHelper = DatabaseHelper(this)

        val prefs: SharedPreferences = getSharedPreferences("user_session", MODE_PRIVATE)
        userId = prefs.getInt("user_id", -1)
        if (userId == -1) return

        setupTopicSpinner()
        setupLevelSpinner()
        setupStatusSpinner()
        setupHeaderClicks()
        loadProgress()
    }

    // Спиннер: Тема 
    private fun setupTopicSpinner() {
        allTopics = dbHelper.getTopics()

        val names = mutableListOf("Все темы")
        names.addAll(allTopics.map { it.name })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        val autoComplete = binding.spinnerTopic as? MaterialAutoCompleteTextView
        autoComplete?.setAdapter(adapter)
        autoComplete?.setText(names[0], false)

        autoComplete?.setOnItemClickListener { _, _, position, _ ->
            selectedTopic = if (position == 0) null else allTopics[position - 1]
            loadProgress()
        }
    }

    // Спиннер: Уровень сложности
    private fun setupLevelSpinner() {
        val levels = listOf("Все уровни", "Простой", "Средний", "Сложный")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, levels)
        val autoComplete = binding.spinnerLevel as? MaterialAutoCompleteTextView
        autoComplete?.setAdapter(adapter)
        autoComplete?.setText(levels[0], false)

        autoComplete?.setOnItemClickListener { _, _, position, _ ->
            selectedLevel = if (position == 0) null else position
            loadProgress()
        }
    }

    // Спиннер: Статус 
    private fun setupStatusSpinner() {
        val statuses = listOf("Все статусы", "Изучаю", "Знаю", "Повторяю")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statuses)
        val autoComplete = binding.spinnerStatus as? MaterialAutoCompleteTextView
        autoComplete?.setAdapter(adapter)
        autoComplete?.setText(statuses[0], false)

        autoComplete?.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = if (position == 0) null else statuses[position]
            loadProgress()
        }
    }

    // Клики по заголовкам: сортировка по столбцу
    private fun setupHeaderClicks() {
        fun toggle(column: SortColumn) {
            sortState = if (sortState.column == column) {
                sortState.copy(ascending = !sortState.ascending)
            } else {
                SortState(column, ascending = true)
            }
            updateHeaderIndicators()
            loadProgress()
        }

        binding.headerTerm.setOnClickListener        { toggle(SortColumn.TERM) }
        binding.headerDifficulty.setOnClickListener  { toggle(SortColumn.DIFFICULTY) }
        binding.headerStatus.setOnClickListener      { toggle(SortColumn.STATUS) }
        binding.headerRepetitions.setOnClickListener { toggle(SortColumn.REPETITIONS) }
        binding.headerTopic.setOnClickListener       { toggle(SortColumn.TOPIC) }
    }

    // Обновление стрелок при сортировке
    private fun updateHeaderIndicators() {
        val views = mapOf(
            SortColumn.TERM        to binding.headerTerm,
            SortColumn.DIFFICULTY  to binding.headerDifficulty,
            SortColumn.STATUS      to binding.headerStatus,
            SortColumn.REPETITIONS to binding.headerRepetitions,
            SortColumn.TOPIC       to binding.headerTopic
        )

        views.forEach { (col, view) ->
            val label = HEADER_LABELS[col] ?: ""
            view.text = when {
                sortState.column == col && sortState.ascending  -> "$label ↑"
                sortState.column == col && !sortState.ascending -> "$label ↓"
                else -> label
            }
        }
    }

    // Применение сортировки
    private fun applySort(list: List<ProgressItem>): List<ProgressItem> {
        val asc = sortState.ascending
        return when (sortState.column) {
            SortColumn.NONE        -> list
            SortColumn.TERM        -> if (asc) list.sortedBy   { it.term.lowercase() }
            else     list.sortedByDescending { it.term.lowercase() }
            SortColumn.DIFFICULTY  -> if (asc) list.sortedBy   { it.difficulty }
            else     list.sortedByDescending { it.difficulty }
            SortColumn.STATUS      -> {
                val order = STATUS_SORT_ORDER
                if (asc) list.sortedBy        { order[it.status] ?: 99 }
                else     list.sortedByDescending { order[it.status] ?: -1 }
            }
            SortColumn.REPETITIONS -> if (asc) list.sortedBy   { it.number }
            else     list.sortedByDescending { it.number }
            SortColumn.TOPIC       -> if (asc) list.sortedBy   { it.topicName.lowercase() }
            else     list.sortedByDescending { it.topicName.lowercase() }
        }
    }

    // Загрузка и отображение данных 
    private fun loadProgress() {
        var list = if (selectedTopic == null) {
            dbHelper.getUserProgressWithConcept(userId)
        } else {
            dbHelper.getUserProgressWithConceptByTopic(userId, selectedTopic!!.id)
        }

        selectedLevel?.let { level ->
            list = list.filter { it.difficulty == level }
        }

        selectedStatus?.let { status ->
            list = list.filter { it.status == status }
        }

        val sorted = applySort(list)

        // Отображение столбца «Тема» только если тема не выбрана
        val showTopicColumn = (selectedTopic == null)
        binding.headerTopic.visibility = if (showTopicColumn) View.VISIBLE else View.GONE

        android.util.Log.d("PROGRESS", "Отображено строк: ${sorted.size} | тема=$selectedTopic | уровень=$selectedLevel | статус=$selectedStatus | сорт=$sortState")

        recyclerView.adapter = ProgressAdapter(sorted, showTopicColumn)
    }

    // Модель данных 
    data class ProgressItem(
        val term: String,
        val difficulty: Int,
        val topicId: Int,
        val topicName: String,
        val status: String,
        val number: Int
    )

    // Адаптер RecyclerView 
    class ProgressAdapter(
        private val data: List<ProgressItem>,
        private val showTopicColumn: Boolean
    ) : RecyclerView.Adapter<ProgressAdapter.ProgressViewHolder>() {

        class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTerm:       android.widget.TextView = itemView.findViewById(R.id.tvTerm)
            val tvDifficulty: android.widget.TextView = itemView.findViewById(R.id.tvDifficulty)
            val tvStatus:     android.widget.TextView = itemView.findViewById(R.id.tvStatus)
            val tvNumber:     android.widget.TextView = itemView.findViewById(R.id.tvNumber)
            val tvTopic:      android.widget.TextView = itemView.findViewById(R.id.tvTopic)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_progress_row, parent, false)
            return ProgressViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
            val item = data[position]

            holder.tvTerm.text = item.term

            holder.tvDifficulty.text = when (item.difficulty) {
                1    -> "Простой"
                2    -> "Средний"
                3    -> "Сложный"
                else -> "—"
            }

            holder.tvStatus.text = item.status
            holder.tvNumber.text = item.number.toString()

            if (showTopicColumn) {
                holder.tvTopic.visibility = View.VISIBLE
                holder.tvTopic.text = item.topicName
            } else {
                holder.tvTopic.visibility = View.GONE
            }
        }

        override fun getItemCount(): Int = data.size
    }
}