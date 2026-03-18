package com.academy.taskflow.data

import com.academy.taskflow.data.local.TaskDao
import com.academy.taskflow.data.local.toDomain
import com.academy.taskflow.data.local.toEntity
import com.academy.taskflow.data.remote.TaskApiService
import com.academy.taskflow.data.remote.toDomain
import com.academy.taskflow.di.IoDispatcher
import com.academy.taskflow.domain.TaskRepository
import com.academy.taskflow.model.Task
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/*
 *
 * DefaultTaskRepository — implementazione offline-first del repository.
 *
 * STRATEGIA OFFLINE-FIRST:
 * La sorgente di verità (Single Source of Truth) è sempre Room.
 * La rete è una sorgente di aggiornamento, non la sorgente primaria.
 * getTasksStream() espone sempre i dati locali — disponibili
 * anche in assenza di connettività.
 * syncFromRemote() aggiorna Room dalla rete — Room notifica
 * il Flow automaticamente → UI aggiornata senza intervento manuale.
 *
 * CONSTRUCTOR INJECTION (preferito a Field Injection):
 * Le dipendenze dichiarate nel costruttore sono esplicite,
 * immutabili (val), e il oggetto è sempre in stato valido.
 * Nei test: si istanzia con FakeRepository senza framework DI.
 *
 */
class DefaultTaskRepository @Inject constructor(
    private val taskDao     : TaskDao,
    private val apiService  : TaskApiService,
    @IoDispatcher
    private val dispatcher  : CoroutineDispatcher
) : TaskRepository {

    /*
     * getTasksStream: non usa withContext — Room gestisce
     * internamente il threading per i Flow tramite InvalidationTracker.
     * Ogni modifica alla tabella "tasks" emette automaticamente
     * la lista aggiornata sul dispatcher corretto.
     */
    override fun getTasksStream(): Flow<List<Task>> =
        taskDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }

    /*
     * withContext(dispatcher): sposta l'esecuzione su Dispatchers.IO.
     * Obbligatorio per operazioni suspend su SQLite —
     * mai eseguire query di database sul Main thread.
     */
    override suspend fun getTaskById(taskId: String): Task? =
        withContext(dispatcher) {
            taskDao.getById(taskId)?.toDomain()
        }

    override suspend fun saveTask(task: Task) =
        withContext(dispatcher) {
            taskDao.upsert(task.toEntity())
        }

    /*
     * toggleDone: operazione atomica — leggi → modifica → scrivi.
     * copy(isCompleted = !current.isCompleted): crea una NUOVA
     * istanza con il campo invertito — il domain model è immutabile.
     * return@withContext: early return tipizzato dal lambda.
     */
    override suspend fun toggleDone(taskId: String) =
        withContext(dispatcher) {
            val current = taskDao.getById(taskId) ?: return@withContext
            taskDao.upsert(current.copy(isCompleted = !current.isCompleted))
        }

    override suspend fun deleteTask(taskId: String) =
        withContext(dispatcher) {
            taskDao.deleteById(taskId)
        }

    /*
     * syncFromRemote: implementazione del pattern offline-first.
     *
     * runCatching: gestisce silenziosamente gli errori di rete.
     * Result<Unit>: tipo algebrico — successo o fallimento esplicito.
     * Il chiamante (ViewModel) decide come gestire il fallimento.
     *
     * .take(20): JSONPlaceholder restituisce 200 task —
     * prendiamo solo i primi 20 per non sovraccaricare il DB.
     *
     * Dopo upsertAll: Room emette automaticamente la lista
     * aggiornata nel Flow di getTasksStream() — la UI si aggiorna.
     *
     */
    override suspend fun syncFromRemote(): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val remote = apiService.getTasks().take(20)
                taskDao.upsertAll(remote.map { it.toDomain().toEntity() })
            }
        }
}
