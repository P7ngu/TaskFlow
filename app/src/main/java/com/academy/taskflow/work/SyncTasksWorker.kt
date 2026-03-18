package com.academy.taskflow.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.academy.taskflow.domain.TaskRepository
import com.academy.taskflow.model.Priority
import com.academy.taskflow.model.Task
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.lang.Exception
import com.academy.taskflow.data.remote.TaskApiService


/*
 * SyncTasksWorker — worker per la sincronizzazione in background.
 *
 * CoroutineWorker: la classe base per i task asincroni con coroutines.
 * doWork() gira su un thread in background — mai sul Main thread.
 *
 * @HiltWorker: permette a Hilt di iniettare dipendenze nel Worker.
 * @AssistedInject: WorkManager crea il Worker con Context e WorkerParameters,
 *   Hilt inietta il resto (TaskRepository).
 *
 * Quando usare WorkManager (e non una semplice coroutine):
 *   - Il task deve completarsi ANCHE se l'app viene chiusa
 *   - Il task deve completarsi ANCHE se il dispositivo viene riavviato
 *   - Il task ha constraints (solo WiFi, solo con batteria carica)
 */
@HiltWorker
class SyncTasksWorker @AssistedInject constructor(
    @Assisted ctx            : Context,
    @Assisted params         : WorkerParameters,
    private val repository   : TaskRepository
) : CoroutineWorker(ctx, params) {

    /*
     * doWork: il lavoro da eseguire in background.
     *
     * Result.success(): completato correttamente
     * Result.failure(): errore definitivo — non riprovare
     * Result.retry():   errore temporaneo — WorkManager riprova
     *                   con backoff esponenziale automatico
     */
    override suspend fun doWork(): Result {
        return try {

            /*
             * Retrofit.Builder(): costruisce il client HTTP.
             *
             * baseUrl: l'URL base di tutte le chiamate. Il path relativo
             * definito in @GET si appende a questo URL.
             * Deve terminare con / — altrimenti Retrofit lancia un'eccezione.
             *
             * GsonConverterFactory: converte automaticamente il JSON della
             * risposta in oggetti Kotlin (TodoDto) e viceversa.
             *
             * NOTA: in produzione Retrofit andrebbe iniettato da Hilt come
             * @Singleton — non ricreato ad ogni sync. Lo facciamo così
             * per semplicità nella demo.
             */
            val service = retrofit2.Retrofit.Builder()
                .baseUrl("https://jsonplaceholder.typicode.com/")
                .addConverterFactory(
                    retrofit2.converter.gson.GsonConverterFactory.create()
                )
                .build()
                .create(TaskApiService::class.java)

            /*
             * getTodos(): chiamata di rete sospendibile.
             * Grazie a suspend, Retrofit esegue la richiesta HTTP su un
             * thread IO e restituisce il risultato al thread chiamante
             * senza bloccare nulla. Se la rete non è disponibile o il
             * server risponde con un errore, lancia un'eccezione che
             * viene catturata dal catch sottostante.
             */
            val todos = service.getTasks()

            /*
             * forEach: itera su ogni TodoDto ricevuto e lo salva in Room.
             *
             * dto.id.toString(): l'id del server è Int, ma Task.id è String.
             * La conversione garantisce compatibilità con il modello esistente.
             *
             * OnConflictStrategy.REPLACE nel DAO (già configurato):
             * se un task con lo stesso id esiste già in Room, viene
             * sovrascritto con i dati aggiornati dal server.
             */
            todos.forEach { dto ->
                repository.saveTask(
                    com.academy.taskflow.model.Task(
                        id          = dto.id.toString(),
                        title       = dto.title,
                        isCompleted = dto.completed,
                        priority    = com.academy.taskflow.model.Priority.LOW
                    )
                )
            }

            /*
             * Result.success(): comunica a WorkManager che il job è terminato
             * correttamente. WorkManager non lo rieseguirà.
             */
            Result.success()

        } catch (e: Exception) {

            /*
             * Il catch cattura QUALSIASI eccezione:
             *   - IOException: rete assente o connessione interrotta
             *   - HttpException: server risponde con 4xx o 5xx
             *   - JsonParseException: risposta malformata
             *   - Qualsiasi altro errore imprevisto
             *
             * Log.e: logga l'errore in Logcat con stack trace completo.
             * Visibile in AS > View > Tool Windows > Logcat filtrando "SyncWorker".
             *
             * Result.retry(): comunica a WorkManager che il job è fallito
             * temporaneamente. WorkManager lo rieseguirà automaticamente
             * con backoff esponenziale — aspetta sempre più a lungo
             * tra un tentativo e l'altro.
             * Diverso da Result.failure() che è un fallimento permanente
             * e non viene mai riprovato.
             */
            android.util.Log.e("SyncWorker", "Errore sincronizzazione", e)
            Result.retry()
        }
    }


    companion object {
        /* Nome univoco del worker — usato da WorkManager per deduplicare */
        const val WORK_NAME = "sync_tasks_worker"
    }
}
