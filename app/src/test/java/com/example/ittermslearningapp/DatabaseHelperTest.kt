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

    // Количество тем в базе данных
    @Test
    fun getTopics_returnsExactlySixTopics() {
        val topics = db.getTopics()
        assertEquals(
            "Ожидается ровно 6 тем в базе данных",
            6, topics.size
        )
    }

    // Получение случайного концепта — корректные параметры
    @Test
    fun getRandomConcept_validDifficultyAndTopic_returnsNonNull() {
        val concept = db.getRandomConcept(difficulty = 1, topicId = 1)
        assertNotNull(
            "getRandomConcept должен вернуть объект для difficulty=1, topicId=1",
            concept
        )
    }

    // Получение концептов по уровню и теме — корректные параметры
    @Test
    fun getConceptsByLevelAndTopic_validParams_returnsNonEmptyList() {
        val concepts = db.getConceptsByLevelAndTopic(level = 1, topicId = 1)
        assertTrue(
            "Список концептов должен быть непустым для level=1, topicId=1",
            concepts.isNotEmpty()
        )
    }

    // Несуществующий уровень сложности
    @Test
    fun getRandomConcept_invalidDifficulty_returnsNull() {
        val concept = db.getRandomConcept(difficulty = 99, topicId = 1)
        assertNull(
            "Должен вернуть null для несуществующего уровня сложности",
            concept
        )
    }

    // Несуществующая тема
    @Test
    fun getConceptsByLevelAndTopic_invalidTopicId_returnsEmptyList() {
        val concepts = db.getConceptsByLevelAndTopic(level = 1, topicId = 999)
        assertTrue(
            "Список должен быть пустым для несуществующей темы topicId=999",
            concepts.isEmpty()
        )
    }

    // getConceptsForTest возвращает не более limit концептов
    @Test
    fun getConceptsForTest_returnsAtMostLimitConcepts() {
        val result = db.getConceptsForTest(topicId = 1, level = 1, limit = 3)
        assertTrue(
            "getConceptsForTest должен вернуть не более 3 концептов",
            result.size <= 3
        )
    }

    // getConceptsForTest с несуществующей темой возвращает пустой список
    @Test
    fun getConceptsForTest_invalidTopic_returnsEmptyList() {
        val result = db.getConceptsForTest(topicId = 999, level = 1, limit = 10)
        assertTrue(
            "getConceptsForTest должен вернуть пустой список для несуществующей темы",
            result.isEmpty()
        )
    }

    // getRelatedConcepts возвращает Map (не падает и не равен null)
    @Test
    fun getRelatedConcepts_validConceptId_returnsMap() {
        // Берём первый доступный концепт из темы 1, уровень 1
        val concept = db.getRandomConcept(difficulty = 1, topicId = 1)
        assertNotNull("Нужен хотя бы один концепт для теста", concept)
        val related = db.getRelatedConcepts(concept!!.id)
        assertNotNull("getRelatedConcepts не должен возвращать null", related)
    }

    // getRelatedConcepts для несуществующего концепта возвращает пустую Map
    @Test
    fun getRelatedConcepts_invalidConceptId_returnsEmptyMap() {
        val related = db.getRelatedConcepts(conceptId = Int.MAX_VALUE)
        assertTrue(
            "Для несуществующего концепта должна вернуться пустая Map",
            related.isEmpty()
        )
    }

    // Поиск несуществующего пользователя
    @Test
    fun getUserByUsername_nonExistentLogin_returnsNull() {
        val user = db.getUserByUsername("__no_such_user_xyz__")
        assertNull(
            "getUserByUsername должен вернуть null для отсутствующего пользователя",
            user
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

    // getUserProgress для несуществующей записи возвращает null
    @Test
    fun getUserProgress_nonExistentRecord_returnsNull() {
        val progress = db.getUserProgress(userId = Int.MAX_VALUE, conceptId = Int.MAX_VALUE)
        assertNull(
            "getUserProgress должен вернуть null, если запись не существует",
            progress
        )
    }

    // insertUserProgress с INSERT OR IGNORE не дублирует запись
    @Test
    fun insertUserProgress_duplicate_doesNotCreateSecondRecord() {
        val testUserId    = 99_998
        val testConceptId = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 1)
        db.insertUserProgress(testUserId, testConceptId, "Знаю",   5) // должен быть проигнорирован
        val progress = db.getUserProgress(testUserId, testConceptId)

        assertNotNull(progress)
        assertEquals("Статус должен остаться от первой вставки", "Изучаю", progress!!.status)
        assertEquals("Счётчик должен остаться от первой вставки", 1, progress.number)
    }

    // incrementViewCount увеличивает счётчик на заданное значение
    @Test
    fun incrementViewCount_increasesNumberByGivenAmount() {
        val testUserId    = 99_997
        val testConceptId = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 2)
        db.incrementViewCount(testUserId, testConceptId, incrementBy = 4)
        val progress = db.getUserProgress(testUserId, testConceptId)

        assertNotNull(progress)
        assertEquals("Счётчик должен быть 2 + 4 = 6", 6, progress!!.number)
    }

    // incrementViewCount по умолчанию увеличивает счётчик на 1
    @Test
    fun incrementViewCount_defaultIncrement_increasesByOne() {
        val testUserId    = 99_996
        val testConceptId = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 0)
        db.incrementViewCount(testUserId, testConceptId) // incrementBy = 1 по умолчанию
        val progress = db.getUserProgress(testUserId, testConceptId)

        assertNotNull(progress)
        assertEquals("Счётчик должен стать 1", 1, progress!!.number)
    }

    // updateStatusAfterTest обновляет статус существующей записи
    @Test
    fun updateStatusAfterTest_changesStatusCorrectly() {
        val testUserId    = 99_995
        val testConceptId = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 1)
        db.updateStatusAfterTest(testUserId, testConceptId, "Знаю")
        val progress = db.getUserProgress(testUserId, testConceptId)

        assertNotNull(progress)
        assertEquals("Статус должен обновиться на 'Знаю'", "Знаю", progress!!.status)
    }

    // upsertProgressAfterTest — INSERT, если записи не было
    @Test
    fun upsertProgressAfterTest_noExistingRecord_insertsNewRecord() {
        val testUserId    = 99_994
        val testConceptId = 1

        db.upsertProgressAfterTest(testUserId, testConceptId, "Знаю")
        val progress = db.getUserProgress(testUserId, testConceptId)

        assertNotNull("Запись должна быть создана через upsert", progress)
        assertEquals("Статус должен быть 'Знаю'", "Знаю", progress!!.status)
    }

    // upsertProgressAfterTest — UPDATE, если запись уже есть
    @Test
    fun upsertProgressAfterTest_existingRecord_updatesStatus() {
        val testUserId    = 99_993
        val testConceptId = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 2)
        db.upsertProgressAfterTest(testUserId, testConceptId, "Знаю")
        val progress = db.getUserProgress(testUserId, testConceptId)

        assertNotNull(progress)
        assertEquals("Статус должен обновиться через upsert", "Знаю", progress!!.status)
        // Счётчик не должен сброситься
        assertEquals("Счётчик должен остаться 2", 2, progress.number)
    }

    // applyLearningProgressBatch — INSERT новых записей пакетом
    @Test
    fun applyLearningProgressBatch_newRecords_insertsAll() {
        val testUserId = 99_992
        val concept1   = db.getRandomConcept(difficulty = 1, topicId = 1)
        assertNotNull(concept1)

        db.applyLearningProgressBatch(testUserId, mapOf(concept1!!.id to 5))

        val progress = db.getUserProgress(testUserId, concept1.id)
        assertNotNull("Запись должна быть создана через batch", progress)
        assertEquals("Счётчик должен быть 5", 5, progress!!.number)
        assertEquals("Статус должен быть 'Изучаю'", "Изучаю", progress.status)
    }

    // applyLearningProgressBatch — INCREMENT существующих записей пакетом
    @Test
    fun applyLearningProgressBatch_existingRecords_incrementsCounter() {
        val testUserId = 99_991
        val concept1   = db.getRandomConcept(difficulty = 1, topicId = 1)
        assertNotNull(concept1)

        db.insertUserProgress(testUserId, concept1!!.id, "Изучаю", 3)
        db.applyLearningProgressBatch(testUserId, mapOf(concept1.id to 7))

        val progress = db.getUserProgress(testUserId, concept1.id)
        assertNotNull(progress)
        assertEquals("Счётчик должен быть 3 + 7 = 10", 10, progress!!.number)
    }

    // getUserProgressWithConcept — возвращает непустой список после вставки
    @Test
    fun getUserProgressWithConcept_afterInsert_returnsNonEmptyList() {
        val testUserId    = 99_990
        val testConceptId = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 1)
        val list = db.getUserProgressWithConcept(testUserId)

        assertTrue(
            "getUserProgressWithConcept должен вернуть хотя бы одну запись",
            list.isNotEmpty()
        )
    }

    // getUserProgressWithConcept — возвращает пустой список для нового пользователя
    @Test
    fun getUserProgressWithConcept_noProgress_returnsEmptyList() {
        val list = db.getUserProgressWithConcept(userId = Int.MAX_VALUE - 1)
        assertTrue(
            "getUserProgressWithConcept должен вернуть пустой список, если прогресса нет",
            list.isEmpty()
        )
    }

    // getUserProgressWithConceptByTopic — фильтрует по теме корректно
    @Test
    fun getUserProgressWithConceptByTopic_afterInsert_returnsOnlyMatchingTopic() {
        val testUserId    = 99_989
        val testConceptId = 1  // topic_id = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 1)
        val list = db.getUserProgressWithConceptByTopic(testUserId, topicId = 1)

        assertTrue(
            "Список должен быть непустым для topicId=1 после вставки",
            list.isNotEmpty()
        )
        assertTrue(
            "Все записи должны принадлежать topicId=1",
            list.all { it.topicId == 1 }
        )
    }

    // getUserProgressWithConceptByTopic — пустой список для несуществующкй темы
    @Test
    fun getUserProgressWithConceptByTopic_wrongTopic_returnsEmptyList() {
        val testUserId    = 99_988
        val testConceptId = 1  // topic_id = 1

        db.insertUserProgress(testUserId, testConceptId, "Изучаю", 1)
        val list = db.getUserProgressWithConceptByTopic(testUserId, topicId = 999)

        assertTrue(
            "Список должен быть пустым для несуществующей темы",
            list.isEmpty()
        )
    }
}