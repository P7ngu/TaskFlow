package com.academy.taskflow.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.academy.taskflow.data.local.TaskDao
import com.academy.taskflow.data.local.TaskDatabase
import com.academy.taskflow.data.local.TaskEntity
import com.academy.taskflow.model.Priority
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/*
 *
 * TaskDaoTest — Integration Test del Data Access Object.
 *
 * Categoria nella Test Pyramid (Mike Cohn, 2009):
 * Integration Test — verifica l'integrazione tra DAO e SQLite.
 *
 * Room.inMemoryDatabaseBuilder: crea un database SQLite in RAM.
 * Caratteristiche che soddisfano il principio F.I.R.S.T.:
 *   Fast:        ordini di grandezza più veloce del DB su disco
 *   Independent: ogni test crea e distrugge il proprio DB
 *   Repeatable:  nessuno stato persistente tra test
 *
 * allowMainThreadQueries(): necessario per i test — in produzione
 * MAI eseguire query sul Main thread (causa NetworkOnMainThreadException
 * equivalente per SQLite).
 *
 * @RunWith(AndroidJUnit4::class): esegue sul device/emulatore.
 * I test Room sono instrumentation test — richiedono Android runtime.
 *
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database : TaskDatabase
    private lateinit var dao      : TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.taskDao()
    }

    @After
    fun tearDown() {
        /* Chiude il database in-memory — libera la memoria */'
        database.close()
    }

    /* ── TEST 1: upsert e observe ───────────────────────────── */

    @Test
    fun upsert_inserisce_e_observeAll_emette_lista_aggiornata() =
        runTest {
            val entity = TaskEntity(
                id          = "1",
                title       = "Test Task",
                description = "",
                priority    = Priority.HIGH.name,
                isCompleted = false
            )

            dao.upsert(entity)

            /*
             * observeAll().first(): raccoglie il primo valore emesso.
             * Room ha già eseguito la query e notificato il Flow
             * dopo l'upsert — il primo valore contiene il task inserito.
             */
            val result = dao.observeAll().first()
            assertEquals("Deve esserci esattamente 1 task", 1, result.size)
            assertEquals("Il titolo deve corrispondere",
                "Test Task", result[0].title)
            assertFalse("isCompleted deve essere false",
                result[0].isCompleted)
        }

    /* ── TEST 2: upsert aggiorna se id esiste ───────────────── */

    @Test
    fun upsert_aggiorna_task_esistente_con_stesso_id() = runTest {
        val original = TaskEntity("1", "Titolo originale",
            "", Priority.LOW.name, false)
        dao.upsert(original)

        /* Upsert con stesso id — deve aggiornare, non inserire un duplicato */
        val updated = original.copy(title = "Titolo aggiornato",
            isCompleted = true)
        dao.upsert(updated)

        val result = dao.observeAll().first()
        assertEquals("Deve esserci ancora 1 solo task", 1, result.size)
        assertEquals("Il titolo deve essere aggiornato",
            "Titolo aggiornato", result[0].title)
        assertTrue("isCompleted deve essere true", result[0].isCompleted)
    }

    /* ── TEST 3: getById restituisce il task corretto ────────── */

    @Test
    fun getById_restituisce_task_con_id_corretto() = runTest {
        val entity1 = TaskEntity("1", "Task 1", "", Priority.HIGH.name, false)
        val entity2 = TaskEntity("2", "Task 2", "", Priority.LOW.name, false)
        dao.upsert(entity1)
        dao.upsert(entity2)

        val found = dao.getById("1")
        assertNotNull("getById deve trovare il task con id 1", found)
        assertEquals("Deve restituire il task con id 1", "1", found?.id)
        assertEquals("Il titolo deve essere Task 1", "Task 1", found?.title)
    }

    /* ── TEST 4: getById con id inesistente ─────────────────── */

    @Test
    fun getById_restituisce_null_per_id_inesistente() = runTest {
        val result = dao.getById("id-non-esistente")
        assertNull("getById deve restituire null per id inesistente", result)
    }

    /* ── TEST 5: updateCompleted modifica solo isCompleted ────── */

    @Test
    fun updateCompleted_modifica_solo_campo_isCompleted() = runTest {
        val entity = TaskEntity("1", "Task", "Descrizione",
            Priority.MEDIUM.name, false)
        dao.upsert(entity)

        dao.updateCompleted("1", true)

        val result = dao.getById("1")
        assertTrue("isCompleted deve essere true", result?.isCompleted == true)
        /* Verifica che gli altri campi non siano stati toccati */
        assertEquals("Il titolo non deve cambiare", "Task", result?.title)
        assertEquals("La descrizione non deve cambiare",
            "Descrizione", result?.description)
    }

    /* ── TEST 6: deleteById rimuove il task ─────────────────── */

    @Test
    fun deleteById_rimuove_il_task_dalla_tabella() = runTest {
        val entity = TaskEntity("1", "Da eliminare", "", Priority.LOW.name, false)
        dao.upsert(entity)

        dao.deleteById("1")

        val result = dao.observeAll().first()
        assertTrue("La lista deve essere vuota dopo la cancellazione",
            result.isEmpty())
    }

    /* ── TEST 7: upsertAll inserisce lista completa ──────────── */

    @Test
    fun upsertAll_inserisce_lista_di_task() = runTest {
        val entities = listOf(
            TaskEntity("1", "Task 1", "", Priority.HIGH.name, false),
            TaskEntity("2", "Task 2", "", Priority.MEDIUM.name, false),
            TaskEntity("3", "Task 3", "", Priority.LOW.name, true)
        )

        dao.upsertAll(entities)

        val result = dao.observeAll().first()
        assertEquals("Devono esserci 3 task", 3, result.size)
    }
}
