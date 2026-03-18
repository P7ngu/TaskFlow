package com.academy.taskflow.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.academy.taskflow.model.Priority
import com.academy.taskflow.model.Task
import com.academy.taskflow.ui.tasks.TaskListScreen
import com.academy.taskflow.ui.theme.TaskFlowTheme
import com.academy.taskflow.viewmodel.TaskUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskListScreenTest {

    // Regola fondamentale: permette di interagire con il mondo Compose nei test
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * TEST 1: Verifica che nel caricamento appaia il loader.
     * Sostituiti TODO() con valori neutri per evitare crash.
     */
    @Test
    fun stato_Loading_mostra_indicatore_di_caricamento() {
        composeTestRule.setContent {
            TaskFlowTheme {
                TaskListScreen(
                    uiState = TaskUiState.Loading,
                    onTaskClick = {},
                    onToggleDone = {},
                    showCompleted = true,       // Mock: mostriamo i completati di default
                    onToggleFilter = {},        // Callback vuota
                    syncState = null,           // Nessuna sincronizzazione in corso
                    onSyncNow = {},
                    onAddTask = {},
                    snackbarHostState = remember { SnackbarHostState() } // Stato finto per la snackbar
                )
            }
        }

        // Cerca un nodo che ha la descrizione "Caricamento task in corso" (quella che abbiamo messo nel file UI)
        composeTestRule
            .onNodeWithContentDescription("Caricamento task in corso")
            .assertExists()
    }

    /**
     * TEST 2: Verifica la schermata vuota.
     */
    @Test
    fun stato_Empty_mostra_messaggio_nessun_task() {
        composeTestRule.setContent {
            TaskFlowTheme {
                TaskListScreen(
                    uiState = TaskUiState.Empty,
                    onTaskClick = {},
                    onToggleDone = {},
                    showCompleted = true,
                    onToggleFilter = {},
                    syncState = null,
                    onSyncNow = {},
                    onAddTask = {},
                    snackbarHostState = remember { SnackbarHostState() }
                )
            }
        }

        // Verifica che il testo "Nessun task" sia visibile a schermo
        composeTestRule
            .onNodeWithText("Nessun task")
            .assertIsDisplayed()
    }

    /**
     * TEST 3: Verifica il rendering della lista.
     */
    @Test
    fun stato_Success_mostra_titoli_dei_task() {
        // Creiamo dati mock (finti) per il test
        val tasks = listOf(
            Task(title = "Setup Android Studio", priority = Priority.HIGH),
            Task(title = "Imparare Compose",     priority = Priority.MEDIUM)
        )

        composeTestRule.setContent {
            TaskFlowTheme {
                TaskListScreen(
                    uiState = TaskUiState.Success(tasks),
                    onTaskClick = {},
                    onToggleDone = {},
                    showCompleted = true,
                    onToggleFilter = {},
                    syncState = null,
                    onSyncNow = {},
                    onAddTask = {},
                    snackbarHostState = remember { SnackbarHostState() }
                )
            }
        }

        // Per ogni task finto, controlliamo che il titolo appaia nella UI
        tasks.forEach { task ->
            composeTestRule
                .onNodeWithText(task.title)
                .assertIsDisplayed()
        }
    }

    /**
     * TEST 5: Verifica il pattern "Events Up".
     * Quando clicco, la callback deve restituire l'ID giusto.
     */
    @Test
    fun tap_su_task_invoca_onTaskClick_con_id_corretto() {
        val task = Task(title = "Task cliccabile", priority = Priority.MEDIUM)
        var clickedId: String? = null // Variabile "spia" per catturare l'evento

        composeTestRule.setContent {
            TaskFlowTheme {
                TaskListScreen(
                    uiState = TaskUiState.Success(listOf(task)),
                    onTaskClick = { id -> clickedId = id }, // Catturiamo l'id al click
                    onToggleDone = {},
                    showCompleted = true,
                    onToggleFilter = {},
                    syncState = null,
                    onSyncNow = {},
                    onAddTask = {},
                    snackbarHostState = remember { SnackbarHostState() }
                )
            }
        }

        // Azione: Clicchiamo sul task
        composeTestRule
            .onNodeWithText("Task cliccabile")
            .performClick()

        // Verifica: La nostra variabile spia è stata aggiornata correttamente?
        assert(clickedId == task.id) {
            "ERRORE: onTaskClick non ha restituito l'ID corretto!"
        }
    }
}