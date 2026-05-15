package com.example.ittermslearningapp

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class DatabaseHelperTest {

    private lateinit var context: Context
    private lateinit var db: DatabaseHelper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = DatabaseHelper(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // Проверка количества тем
    @Test
    fun getTopics_returnsExactlySixTopics() {
        val topics = db.getTopics()
        assertEquals(
            "Ожидается ровно 6 тем в базе данных",
            6, topics.size
        )
    }

    // Получение случайного концепта
    @Test
    fun getRandomConcept_validDifficultyAndTopic_returnsNonNull() {
        val concept = db.getRandomConcept(difficulty = 1, topicId = 1)
        assertNotNull(
            "getRandomConcept должен вернуть объект для difficulty=1, topicId=1",
            concept
        )
    }

    // Получение концептов по уровню
    @Test
    fun getConceptsByLevelAndTopic_validParams_returnsNonEmptyList() {
        val concepts = db.getConceptsByLevelAndTopic(level = 1, topicId = 1)
        assertTrue(
            "Список концептов должен быть непустым для level=1, topicId=1",
            concepts.isNotEmpty()
        )
    }

    // Вставка и чтение прогресса
    @Test
    fun insertUserProgress_thenGetUserProgress_returnsMatchingRecord() {
        val testUserId    = 99_999
        val testConceptId = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 3)
        val progress = db.getUserProgress(testUserId, testConceptId)

        assertNotNull("Запись должна быть найдена после вставки", progress)
        assertEquals("Статус должен совпадать", "Изучаю", progress!!.status)
        assertEquals("Счётчик просмотров должен быть 3", 3, progress.number)
    }

    // Обработка несуществующей сложности
    @Test
    fun getRandomConcept_invalidDifficulty_returnsNull() {
        val concept = db.getRandomConcept(difficulty = 99, topicId = 1)
        assertNull(
            "Должен вернуть null для несуществующего уровня сложности",
            concept
        )
    }

    // Обработка несуществующей темы
    @Test
    fun getConceptsByLevelAndTopic_invalidTopicId_returnsEmptyList() {
        val concepts = db.getConceptsByLevelAndTopic(level = 1, topicId = 999)
        assertTrue(
            "Список должен быть пустым для несуществующей темы topicId=999",
            concepts.isEmpty()
        )
    }

    // Поиск отсутствующего пользователя
    @Test
    fun getUserByUsername_nonExistentLogin_returnsNull() {
        val user = db.getUserByUsername("__no_such_user_xyz__")
        assertNull(
            "getUserByUsername должен вернуть null для отсутствующего пользователя",
            user
        )
    }
}