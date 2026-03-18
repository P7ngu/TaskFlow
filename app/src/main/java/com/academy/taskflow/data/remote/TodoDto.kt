package com.academy.taskflow.data.remote

/*
 * TodoDto — Data Transfer Object che rappresenta la risposta dell'API.
 *
 * DTO: classe che rispecchia ESATTAMENTE la struttura JSON ricevuta dal server.
 * Non contiene logica — è solo un contenitore di dati.
 *
 * La risposta di jsonplaceholder per ogni todo è:
 * { "id": 1, "title": "delectus aut autem", "completed": false }
 *
 * I nomi dei campi devono corrispondere esattamente alle chiavi JSON.
 * Se non corrispondono, Gson li ignora e il campo rimane null o al valore default.
 */
data class TodoDto(
    /* id: identificatore univoco del todo sul server.
     * Viene convertito a String per adattarsi al modello Task di TaskFlow. */
    val id       : Int,

    /* title: il testo del task — corrisponde a Task.title. */
    val title    : String,

    /* completed: stato di completamento — corrisponde a Task.isCompleted. */
    val completed: Boolean
)
