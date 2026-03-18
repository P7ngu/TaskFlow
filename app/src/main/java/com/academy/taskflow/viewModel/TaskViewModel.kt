// Definiamo il "namespace" del componente. Segue la convenzione Reverse Domain Name.
package com.academy.taskflow.viewModel

// Importiamo le dipendenze necessarie per il ciclo di vita e la gestione dei flussi asincroni.
import androidx.lifecycle.ViewModel
import com.academy.taskflow.model.Priority
import com.academy.taskflow.model.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * TASK UI STATE: La "Single Source of Truth" per la UI.
 * Utilizziamo una 'sealed class' per rappresentare un ADT (Algebraic Data Type).
 * Questo garantisce l'esaustività: la UI può trovarsi SOLO in uno di questi tre stati.
 */
sealed class TaskUiState {
    // 'object' perché lo stato Loading è un singleton: non ha dati variabili, è solo un segnale.
    object Loading                            : TaskUiState()

    // 'data class' perché lo stato Success trasporta un "payload": la lista dei Task.
    data class Success(val tasks: List<Task>) : TaskUiState()

    // 'object' per lo stato Empty: evita allocazioni inutili quando la lista è vuota.
    object Empty                              : TaskUiState()
}

/**
 * TASK VIEWMODEL: Il mediatore tra il Modello (Dati) e la View (UI).
 * Estende 'ViewModel()' per garantire la sopravvivenza ai Configuration Changes (es. rotazione).
 */
class TaskViewModel : ViewModel() {

    /**
     * IL BACKING PROPERTY PATTERN (Incapsulamento):
     * _uiState è PRIVATO e MUTABILE. Solo questo ViewModel può scriverci.
     * Inizializziamo con Loading perché all'avvio i dati non sono ancora presenti.
     */
    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)

    /**
     * uiState è PUBBLICO e IMMUTABILE (StateFlow).
     * Usiamo asStateFlow() per esporre una versione in sola lettura.
     * Questo impedisce alla View di "manomettere" lo stato dall'esterno.
     */
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    /**
     * Blocco INIT: Eseguito immediatamente all'instanziazione del ViewModel.
     * Innesca il caricamento dei dati non appena il componente "nasce".
     */
    init { loadTasks() }

    /**
     * LOAD TASKS: Simula il recupero dei dati da un database o API.
     * In questa fase accademica usiamo dati "hardcoded" (fissi).
     */
    fun loadTasks() {
        val tasks = listOf(
            Task(title = "Setup Android Studio", priority = Priority.HIGH),
            Task(title = "Imparare Compose", priority = Priority.HIGH),
            Task(title = "Integrare Room", priority = Priority.MEDIUM),
            Task(title = "Configurare Hilt", priority = Priority.MEDIUM),
            Task(title = "Scrivere i test", priority = Priority.LOW)
        )

        // LOGICA DI TRANSIZIONE DI STATO:
        // Usiamo l'if come espressione per determinare il prossimo stato.
        _uiState.value = if (tasks.isEmpty()) TaskUiState.Empty
        else                 TaskUiState.Success(tasks)
    }

    /**
     * TOGGLE DONE: Gestisce l'interazione dell'utente (Intent).
     * Dimostra l'uso dell'immutabilità e della programmazione funzionale.
     */
    fun toggleDone(taskId: String) {
        // 1. SAFE CAST: Se lo stato non è Success, non possiamo invertire nulla -> esci.
        val current = _uiState.value as? TaskUiState.Success ?: return

        // 2. TRASFORMAZIONE IMMUTABILE:
        // .map crea una NUOVA lista. Non modifichiamo la lista esistente (Thread Safety).
        val updated = current.tasks.map { task ->
            // Se non è il task cliccato, restituiscilo così com'è.
            if (task.id != taskId) {
                task
            } else {
                // Se è lui, usa .copy() per creare un nuovo oggetto con valore invertito.
                // Il pattern "copy" è fondamentale per la stabilità della UI in Compose.
                task.copy(isCompleted = !task.isCompleted)
            }
        }

        // 3. EMISSIONE: Aggiorniamo il Flow con il nuovo stato Success contenente la lista aggiornata.
        _uiState.value = TaskUiState.Success(updated)
    }

    /**
     * GET TASK BY ID: Funzione di utility per estrarre dati dallo stato attuale.
     * @return Il task trovato o null se lo stato non è Success o l'ID è errato.
     */
    fun getTaskById(taskId: String): Task? {
        // Accediamo allo stato tramite cast sicuro.
        val current = _uiState.value as? TaskUiState.Success ?: return null

        // Ricerca lineare tramite predicato.
        return current.tasks.firstOrNull { it.id == taskId }
    }
}