package com.academy.taskflow.data.remote

import retrofit2.http.GET

/*
 * TaskApiService — interfaccia che descrive gli endpoint dell'API remota.
 *
 * Retrofit legge queste annotazioni a runtime e genera l'implementazione
 * HTTP automaticamente. Non si scrive mai codice di rete a mano.
 *
 * Ogni funzione annotata corrisponde a una chiamata HTTP:
 *   @GET    → HTTP GET  (lettura)
 *   @POST   → HTTP POST (creazione)
 *   @PUT    → HTTP PUT  (aggiornamento completo)
 *   @DELETE → HTTP DELETE (eliminazione)
 *
 * Il valore dentro l'annotazione è il path relativo alla baseUrl
 * definita in NetworkModule. Esempio:
 *   baseUrl  = "https://jsonplaceholder.typicode.com/"
 *   @GET     = "todos"
 *   URL finale = "https://jsonplaceholder.typicode.com/todos"
 *
 * suspend: la funzione è una coroutine.
 * Retrofit esegue la richiesta HTTP su un thread IO automaticamente —
 * non blocca mai il Main thread, non serve withContext(Dispatchers.IO).
 */
interface TaskApiService {

    /*
     * getTasks: recupera la lista dei task dal server.
     *
     * Restituisce List<TaskDto>: Gson deserializza automaticamente
     * l'array JSON in una lista di oggetti Kotlin.
     *
     * In caso di errore (rete assente, 4xx, 5xx) Retrofit lancia
     * un'eccezione — catturata dal try/catch in SyncTasksWorker.
     */
    @GET("todos")
    suspend fun getTasks(): List<TaskDto>
}