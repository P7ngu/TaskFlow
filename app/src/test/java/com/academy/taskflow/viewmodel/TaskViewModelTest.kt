package com.academy.taskflow.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.academy.taskflow.domain.TaskRepository
import com.academy.taskflow.model.Priority
import com.academy.taskflow.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/*
 * ═══════════════════════════════════════════════════════════════
 * FakeTaskRepository — Test Double di categoria Fake.
 *
 * Nomenclatura secondo Gerard Meszaros ("xUnit Test Patterns", 2007):
 *   Dummy:  oggetto passato ma mai usato
 *   Stub:   restituisce risposte predefinite
 *   Fake:   implementazione funzionante ma semplificata ← questo
 *   Mock:   verifica le interazioni ricevute
 *   Spy:    registra le chiamate per verifica successiva
 *
 * Un Fake è preferibile a un Mock per testare il ViewModel perché:
 *   1. Testa il comportamento osservabile, non le interazioni interne
 *   2. Non è accoppiato all'implementazione specifica del ViewModel
 *   3. È più robusto ai refactoring — cambia il codice, non il test
 *
 * MutableStateFlow simula il comportamento reattivo di Room:
 * ogni modifica emette automaticamente la nuova lista.
 * ═══════════════════════════════════════════════════════════════
 */
class FakeTaskRepository : TaskRepository {
    /*
     * _tasks: stato mutabile interno del Fake.
     * _flow: Stream reattivo che simula Room.observeAll()
     */
    private val _tasks = mutableListOf<Task>()
    private val _flow  = MutableStateFlow<List<Task>>(emptyList())

    override fun getTasksStream() = _flow.asStateFlow()

    override suspend fun saveTask(task: Task) {
        _tasks.add(task)
        _flow.value = _tasks.toList()   /* emette la nuova lista */
    }

    override suspend fun toggleDone(taskId: String) {
        val index = _tasks.indexOfFirst { it.id == taskId }
        if (index >= 0) {
            _tasks[index] = _tasks[index].copy(
                isCompleted = !_tasks[index].isCompleted
            )
            _flow.value = _tasks.toList()
        }
    }

    override suspend fun getTaskById(id: String) =
        _tasks.find { it.id == id }

    override suspend fun deleteTask(id: String) {
        _tasks.removeAll { it.id == id }
        _flow.value = _tasks.toList()
    }

    override suspend fun syncFromRemote() = Result.success(Unit)
}

/*
 * ═══════════════════════════════════════════════════════════════
 * TaskViewModelTest — Unit Test del ViewModel.
 *
 * Principio F.I.R.S.T. (Robert C. Martin — "Clean Code"):
 *   Fast:        esecuzione in millisecondi — nessun I/O reale
 *   Independent: ogni test è isolato — nessuna dipendenza da altri
 *   Repeatable:  stesso risultato in qualsiasi ambiente
 *   Self-validating: pass/fail senza ispezione manuale
 *   Timely:      scritto insieme al codice di produzione
 *
 * @OptIn(ExperimentalCoroutinesApi::class): API di test per coroutine.
 * ═══════════════════════════════════════════════════════════════
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaskViewModelTest {

    /*
     * InstantTaskExecutorRule: esegue le operazioni Architecture Components
     * sincronamente sul thread del test invece di usare thread background.
     * Necessario per testare LiveData e operazioni che usano
     * MainCoroutineDispatcher.
     */
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    /*
     * TestCoroutineDispatcher: sostituisce Dispatchers.Main con un
     * dispatcher controllabile — permette di avanzare il tempo
     * virtualmente e testare operazioni asincrone in modo deterministico.
     */
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository : FakeTaskRepository
    private lateinit var viewModel  : TaskViewModel

    @Before
    fun setup() {
        /*
         * Sostituisce Dispatchers.Main con il TestDispatcher.
         * Senza questo, le coroutine avviate con viewModelScope
         * userebbero Main Looper che non esiste nei test JVM.
         */
        Dispatchers.setMain(testDispatcher)
        repository = FakeTaskRepository()
        viewModel  = TaskViewModel(repository)
    }

    @After
    fun tearDown() {
        /* Ripristina Dispatchers.Main originale dopo ogni test */
        Dispatchers.resetMain()
    }

    /* ── TEST 1: stato iniziale ─────────────────────────────── */

    @Test
    fun `stato iniziale e Loading prima del caricamento dati`() =
        runTest {
            /*
             * Il ViewModel inizia con Loading.
             * Questo garantisce che la UI mostri sempre uno spinner
             * durante il primo caricamento — mai uno stato vuoto inatteso.
             */
            val initialState = TaskViewModel(FakeTaskRepository()).uiState.value
            assertTrue(
                "Stato iniziale deve essere Loading",
                initialState is TaskUiState.Loading
            )
        }

    /* ── TEST 2: stato Empty con repository vuoto ───────────── */

    @Test
    fun `stato diventa Empty quando il repository non ha task`() =
        runTest {
            /* FakeRepository vuoto — seedIfEmpty non trova task da inserire */
            val emptyRepo = FakeTaskRepository()
            val vm = TaskViewModel(emptyRepo)
            advanceUntilIdle()  /* esegue tutte le coroutine pendenti */

            val state = vm.uiState.value
            assertTrue(
                "Stato deve essere Empty con repository vuoto",
                state is TaskUiState.Empty
            )
        }

    /* ── TEST 3: stato Success dopo inserimento ─────────────── */

    @Test
    fun `stato diventa Success quando ci sono task`() = runTest {
        val task = Task(title = "Test Task", priority = Priority.HIGH)
        repository.saveTask(task)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(
            "Stato deve essere Success con task presenti",
            state is TaskUiState.Success
        )
        assertEquals(
            "Success deve contenere il task inserito",
            1,
            (state as TaskUiState.Success).tasks.size
        )
    }

    /* ── TEST 4: toggleDone inverte isCompleted ─────────────── */

    @Test
    fun `toggleDone inverte isCompleted del task`() = runTest {
        val task = Task(title = "Task da completare", priority = Priority.MEDIUM)
        repository.saveTask(task)
        advanceUntilIdle()

        /* Verifica che il task sia inizialmente non completato */
        assertFalse(
            "Task deve essere non completato inizialmente",
            repository.getTaskById(task.id)?.isCompleted ?: true
        )

        viewModel.toggleDone(task.id)
        advanceUntilIdle()

        /* Verifica che il task sia ora completato */
        assertTrue(
            "Task deve essere completato dopo toggleDone",
            repository.getTaskById(task.id)?.isCompleted ?: false
        )
    }

    /* ── TEST 5: toggleDone due volte ripristina lo stato ────── */

    @Test
    fun `toggleDone due volte ripristina lo stato originale`() = runTest {
        val task = Task(title = "Task idempotente", priority = Priority.LOW)
        repository.saveTask(task)
        advanceUntilIdle()

        viewModel.toggleDone(task.id)
        advanceUntilIdle()
        viewModel.toggleDone(task.id)
        advanceUntilIdle()

        /* Idempotenza del doppio toggle */
        assertFalse(
            "Doppio toggle deve ripristinare lo stato originale",
            repository.getTaskById(task.id)?.isCompleted ?: true
        )
    }

    /* ── TEST 6: getTaskById restituisce il task corretto ────── */

    @Test
    fun `getTaskById restituisce il task corretto`() = runTest {
        val task1 = Task(title = "Task 1", priority = Priority.HIGH)
        val task2 = Task(title = "Task 2", priority = Priority.LOW)
        repository.saveTask(task1)
        repository.saveTask(task2)
        advanceUntilIdle()

        val found = viewModel.getTaskById(task1.id)
        assertNotNull("getTaskById deve trovare il task esistente", found)
        assertEquals("Deve restituire il task con l'id corretto",
            task1.id, found?.id)
    }

    /* ── TEST 7: getTaskById con id inesistente ─────────────── */

    @Test
    fun `getTaskById restituisce null per id inesistente`() = runTest {
        advanceUntilIdle()
        val result = viewModel.getTaskById("id-non-esistente")
        assertNull(
            "getTaskById deve restituire null per id inesistente",
            result
        )
    }
}
