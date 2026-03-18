package com.academy.taskflow.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/*
 * TaskDao — Data Access Object per la tabella tasks.
 *
 * @Dao: indica a Room che questa interfaccia contiene le operazioni sul DB.
 * Room genera l'implementazione a compile-time leggendo le annotazioni.
 * Errori nelle query SQL → errore di compilazione, non crash a runtime.
 *
 * Le funzioni suspend si chiamano da una coroutine.
 * Le funzioni che restituiscono Flow sono reactive:
 *   Room le riesegue automaticamente ogni volta che la tabella cambia.
 *   La UI si aggiorna senza nessun reload manuale.
 */
@Dao
interface TaskDao {

    /*
     * observeAll: restituisce tutti i task come Flow.
     * Ogni volta che un task viene inserito, aggiornato o eliminato,
     * Room riesegue la query e emette la nuova lista.
     */
    @Query("SELECT * FROM tasks ORDER BY priority DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    /*
     * getById: legge un singolo task per ID.
     * suspend: operazione asincrona — chiama da una coroutine.
     * Restituisce null se il task non esiste.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getById(taskId: String): TaskEntity?

    /*
     * upsert: inserisce se non esiste, aggiorna se esiste già.
     * @Upsert è disponibile da Room 2.5 — più semplice di @Insert + @Update.
     */
    @Upsert
    suspend fun upsert(task: TaskEntity)

    /*
     * upsertAll: inserisce o aggiorna una lista di task.
     * Usato per sincronizzare dati da remoto (Retrofit → Room).
     */
    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    /*
     * deleteById: elimina un task per ID.
     */
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteById(taskId: String)

    /*
     * updateCompleted: aggiorna solo il campo isCompleted.
     * Più efficiente di riscrivere l'intero task con @Upsert.
     */
    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :taskId")
    suspend fun updateCompleted(taskId: String, completed: Boolean)
}
