package com.example.ittermslearningapp

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random

class DatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "vocabulary.db"
        private const val DB_VERSION = 1
    }

    private var mDatabase: SQLiteDatabase? = null

    init {
        copyDatabaseIfNeeded()
        openDatabase()
    }

    private fun copyDatabaseIfNeeded() {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            val inputStream: InputStream = context.assets.open(DB_NAME)
            val outputStream: OutputStream = FileOutputStream(dbFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.flush()
            outputStream.close()
        }
    }

    // Открытие БД
    private fun openDatabase() {
        val dbFile = context.getDatabasePath(DB_NAME)
        mDatabase = SQLiteDatabase.openDatabase(
            dbFile.path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
    }

    override fun onCreate(db: SQLiteDatabase?) {
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    }

    // БД Таблицы
    data class Topic(
        val id: Int,
        val name: String
    )

    data class Concept(
        val id: Int,
        val term: String,
        val definition: String,
        val difficulty: Int,
        val topicId: Int
    )

    data class User(
        val id: Int,
        val username: String,
        val password: String     // хранит password_hash
    )

    data class UserProgress(
        val id: Int,
        val userId: Int,
        val conceptId: Int,
        val status: String,
        val number: Int
    )

    // TOPICS
    fun getTopics(): List<Topic> {
        val db = mDatabase!!
        val cursor = db.rawQuery("SELECT id, name FROM Topic ORDER BY id", null)
        val list = mutableListOf<Topic>()
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Topic(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    // CONCEPTS
    fun getRandomConcept(difficulty: Int, topicId: Int): Concept? {
        val cursor: Cursor = mDatabase!!.rawQuery(
            "SELECT id, term, definition, difficulty, topic_id FROM Concept WHERE difficulty=? AND topic_id=?",
            arrayOf(difficulty.toString(), topicId.toString())
        )

        return if (cursor.count > 0) {
            val randomIndex = Random.nextInt(cursor.count)
            cursor.moveToPosition(randomIndex)
            val concept = Concept(
                id         = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                term       = cursor.getString(cursor.getColumnIndexOrThrow("term")),
                definition = cursor.getString(cursor.getColumnIndexOrThrow("definition")),
                difficulty = cursor.getInt(cursor.getColumnIndexOrThrow("difficulty")),
                topicId    = cursor.getInt(cursor.getColumnIndexOrThrow("topic_id"))
            )
            cursor.close()
            concept
        } else {
            cursor.close()
            null
        }
    }

    fun getUserProgressWithConceptByTopic(userId: Int, topicId: Int): List<MyProgressActivity.ProgressItem> {
        val db = mDatabase!!
        val cursor = db.rawQuery(
            """
        SELECT c.term, up.status, up.number, c.difficulty, c.topic_id, t.name AS topic_name
        FROM UserProgress up
        JOIN Concept c ON up.concept_id = c.id
        JOIN Topic t ON c.topic_id = t.id
        WHERE up.user_id = ? AND c.topic_id = ?
        """.trimIndent(),
            arrayOf(userId.toString(), topicId.toString())
        )

        val list = mutableListOf<MyProgressActivity.ProgressItem>()
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    MyProgressActivity.ProgressItem(
                        term       = cursor.getString(cursor.getColumnIndexOrThrow("term")),
                        difficulty = cursor.getInt(cursor.getColumnIndexOrThrow("difficulty")),
                        topicId    = cursor.getInt(cursor.getColumnIndexOrThrow("topic_id")),
                        topicName  = cursor.getString(cursor.getColumnIndexOrThrow("topic_name")),
                        status     = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        number     = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getConceptsByLevelAndTopic(level: Int, topicId: Int): List<Concept> {
        val db = mDatabase!!
        val cursor = db.rawQuery(
            "SELECT id, term, definition, difficulty, topic_id FROM Concept WHERE difficulty = ? AND topic_id = ?",
            arrayOf(level.toString(), topicId.toString())
        )
        val list = mutableListOf<Concept>()
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Concept(
                        id         = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        term       = cursor.getString(cursor.getColumnIndexOrThrow("term")),
                        definition = cursor.getString(cursor.getColumnIndexOrThrow("definition")),
                        difficulty = cursor.getInt(cursor.getColumnIndexOrThrow("difficulty")),
                        topicId    = cursor.getInt(cursor.getColumnIndexOrThrow("topic_id"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getConceptsForTest(topicId: Int, level: Int, limit: Int = 10): List<Concept> {
        val all = getConceptsByLevelAndTopic(level, topicId).toMutableList()
        all.shuffle()
        return all.take(limit)
    }

    fun getRelatedConcepts(conceptId: Int): Map<String, List<Concept>> {
        val db = mDatabase!!
        val result = LinkedHashMap<String, MutableList<Concept>>()

        // Пара: (метка когда термин = source, метка когда термин = target)
        val relationLabels = linkedMapOf(
            "Наследование" to Pair("Расширяет понятие",        "Является родительским понятием для"),
            "Реализация"   to Pair("Реализует понятие",         "Реализован в"),
            "Ассоциация"   to Pair("Связан с",          "Связан с"),
            "Зависимость"  to Pair("Зависит от",        "Требуется для"),
            "Агрегация"    to Pair("Включает",          "Входит в"),
            "Композиция"   to Pair("Состоит из",        "Является частью")
        )

        fun fetchConcepts(query: String, args: Array<String>): List<Concept> {
            val cursor = db.rawQuery(query, args)
            val list = mutableListOf<Concept>()
            if (cursor.moveToFirst()) {
                do {
                    list.add(
                        Concept(
                            id         = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                            term       = cursor.getString(cursor.getColumnIndexOrThrow("term")),
                            definition = cursor.getString(cursor.getColumnIndexOrThrow("definition")),
                            difficulty = cursor.getInt(cursor.getColumnIndexOrThrow("difficulty")),
                            topicId    = cursor.getInt(cursor.getColumnIndexOrThrow("topic_id"))
                        )
                    )
                } while (cursor.moveToNext())
            }
            cursor.close()
            return list
        }

        for ((typeName, labels) in relationLabels) {
            // Термин — source, связанный — target
            val sourceQuery = """
            SELECT c.id, c.term, c.definition, c.difficulty, c.topic_id
            FROM Concept c
            JOIN ConceptRelation cr ON c.id = cr.target_concept_id
            JOIN RelationType rt    ON cr.relation_type_id = rt.id
            WHERE cr.source_concept_id = ? AND rt.name = ?
        """.trimIndent()
            val sourceList = fetchConcepts(sourceQuery, arrayOf(conceptId.toString(), typeName))

            // Термин — target, связанный — source
            val targetQuery = """
            SELECT c.id, c.term, c.definition, c.difficulty, c.topic_id
            FROM Concept c
            JOIN ConceptRelation cr ON c.id = cr.source_concept_id
            JOIN RelationType rt    ON cr.relation_type_id = rt.id
            WHERE cr.target_concept_id = ? AND rt.name = ?
        """.trimIndent()
            val targetList = fetchConcepts(targetQuery, arrayOf(conceptId.toString(), typeName))

            if (sourceList.isNotEmpty()) {
                result.getOrPut(labels.first)  { mutableListOf() }.addAll(sourceList)
            }
            if (targetList.isNotEmpty()) {
                result.getOrPut(labels.second) { mutableListOf() }.addAll(targetList)
            }
        }

        // Дедупликация по id внутри каждой категории
        return result.mapValues { (_, list) ->
            list.distinctBy { it.id }
        }
    }

    // USERS
    fun getUserByUsername(username: String): User? {
        val cursor = mDatabase!!.rawQuery(
            "SELECT id, username, password_hash FROM User WHERE username = ?",
            arrayOf(username)
        )

        if (!cursor.moveToFirst()) {
            cursor.close()
            return null
        }

        val user = User(
            id       = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
            username = cursor.getString(cursor.getColumnIndexOrThrow("username")),
            password = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"))
        )

        cursor.close()
        return user
    }

    // USER PROGRESS
    fun getUserProgress(userId: Int, conceptId: Int): UserProgress? {
        val db = mDatabase!!
        val cursor = db.rawQuery(
            """
            SELECT id, user_id, concept_id, status, number
            FROM UserProgress
            WHERE user_id = ? AND concept_id = ?
            """.trimIndent(),
            arrayOf(userId.toString(), conceptId.toString())
        )

        val result = if (cursor.moveToFirst()) {
            UserProgress(
                id        = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                userId    = cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                conceptId = cursor.getInt(cursor.getColumnIndexOrThrow("concept_id")),
                status    = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                number    = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
            )
        } else null

        cursor.close()
        return result
    }

    fun insertUserProgress(userId: Int, conceptId: Int, status: String, number: Int) {
        val db = mDatabase!!
        db.execSQL(
            """
            INSERT OR IGNORE INTO UserProgress (user_id, concept_id, status, number)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            arrayOf(userId, conceptId, status, number)
        )
    }

    fun incrementViewCount(userId: Int, conceptId: Int, incrementBy: Int = 1) {
        val db = mDatabase!!
        db.execSQL(
            """
            UPDATE UserProgress
            SET number = number + ?
            WHERE user_id = ? AND concept_id = ?
            """.trimIndent(),
            arrayOf(incrementBy, userId, conceptId)
        )
    }

    fun updateStatusAfterTest(userId: Int, conceptId: Int, newStatus: String) {
        val db = mDatabase!!
        db.execSQL(
            """
            UPDATE UserProgress
            SET status = ?
            WHERE user_id = ? AND concept_id = ?
            """.trimIndent(),
            arrayOf(newStatus, userId, conceptId)
        )
    }

    // UPSERT после ответа в тесте
    fun upsertProgressAfterTest(userId: Int, conceptId: Int, status: String) {
        val existing = getUserProgress(userId, conceptId)
        if (existing == null) {
            insertUserProgress(userId, conceptId, status, 1)
        } else {
            updateStatusAfterTest(userId, conceptId, status)
        }
    }

    // обновление из LearningActivity
    fun applyLearningProgressBatch(userId: Int, views: Map<Int, Int>) {
        val db = mDatabase!!
        db.beginTransaction()
        try {
            for ((conceptId, count) in views) {
                val existing = getUserProgress(userId, conceptId)
                if (existing == null) {
                    insertUserProgress(userId, conceptId, "Изучаю", count)
                } else {
                    incrementViewCount(userId, conceptId, count)
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getUserProgressWithConcept(userId: Int): List<MyProgressActivity.ProgressItem> {
        val db = mDatabase!!
        val cursor = db.rawQuery(
            """
        SELECT c.term, up.status, up.number, c.difficulty, c.topic_id, t.name AS topic_name
        FROM UserProgress up
        JOIN Concept c ON up.concept_id = c.id
        JOIN Topic t ON c.topic_id = t.id
        WHERE up.user_id = ?
        """.trimIndent(),
            arrayOf(userId.toString())
        )

        val list = mutableListOf<MyProgressActivity.ProgressItem>()
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    MyProgressActivity.ProgressItem(
                        term       = cursor.getString(cursor.getColumnIndexOrThrow("term")),
                        difficulty = cursor.getInt(cursor.getColumnIndexOrThrow("difficulty")),
                        topicId    = cursor.getInt(cursor.getColumnIndexOrThrow("topic_id")),
                        topicName  = cursor.getString(cursor.getColumnIndexOrThrow("topic_name")),
                        status     = cursor.getString(cursor.getColumnIndexOrThrow("status")),
                        number     = cursor.getInt(cursor.getColumnIndexOrThrow("number"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
}
