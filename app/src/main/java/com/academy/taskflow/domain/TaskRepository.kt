package com.academy.taskflow.domain

import com.academy.taskflow.model.Task
import kotlinx.coroutines.flow.Flow

/*
 * TaskRepository — l'interfaccia del repository dei task.
 *
 * Perché un'interfaccia?
 *   - Il ViewModel dipende dall'interfaccia, non dall'implementazione.
 *   - Nei test si può sostituire l'implementazione reale con una fake.
 *   - Disaccoppia il ViewModel dal data layer (Room, Retrofit).
 *
 * Il ViewModel non sa se i dati vengono dal database locale,
 * dalla rete o da un file — sa solo che può chiamare queste funzioni.
 *
 * Dependency Inversion Principle: dipendi dall'astrazione, non dal dettaglio.
 */
interface TaskRepository {

    /* Restituisce tutti i task come Flow — aggiornato automaticamente */
    fun getTasksStream(): Flow<List<Task>>

    /* Legge un singolo task per ID */
    suspend fun getTaskById(taskId: String): Task?

    /* Salva o aggiorna un task */
    suspend fun saveTask(task: Task)

    /* Cambia lo stato di completamento di un task */
    suspend fun toggleDone(taskId: String)

    /* Elimina un task */
    suspend fun deleteTask(taskId: String)

    /* Sincronizza i task dalla rete */
    suspend fun syncFromRemote(): Result<Unit>
}
