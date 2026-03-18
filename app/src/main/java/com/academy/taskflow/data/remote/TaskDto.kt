package com.academy.taskflow.data.remote

import com.academy.taskflow.model.Priority
import com.academy.taskflow.model.Task

/*
 * TaskDto — Data Transfer Object per la comunicazione con l'API remota.
 *
 * PATTERN DTO (Fowler — Patterns of Enterprise Application Architecture):
 * Rispecchia ESATTAMENTE la struttura JSON del server.
 * Separato dal domain model Task perché i due hanno assi di cambiamento
 * ortogonali: il server può cambiare struttura senza impattare la logica
 * di business dell'app, e viceversa.
 *
 * JSONPlaceholder risponde con:
 * { "id": 1, "title": "delectus aut autem", "completed": false }
 *
 * Gson mappa automaticamente i campi per nome.
 * Se il nome JSON differisce da quello Kotlin usare:
 * @SerializedName("nome_json") val nomeKotlin: Tipo
 */
data class TaskDto(
    /* id: Int sul server — convertito a String nel domain model
     * perché Task.id è String per compatibilità con UUID generati localmente */
    val id        : Int,

    /* title: il testo del task — corrisponde a Task.title */
    val title     : String,

    /* completed: stato completamento — corrisponde a Task.isCompleted */
    val completed : Boolean
)

/*
 * toDomain: mapper DTO → domain model.
 *
 * Extension function: si chiama come metodo su TaskDto.
 * Vive nello stesso file del DTO per coesione — tutti i dettagli
 * della conversione sono in un posto solo.
 *
 * id.toString(): converte Int in String.
 * priority = MEDIUM: l'API non espone la priorità — usiamo MEDIUM come default.
 * description = "": l'API non espone la descrizione — stringa vuota come default.
 */
fun TaskDto.toDomain() = Task(
    id          = id.toString(),
    title       = title,
    isCompleted = completed,
    priority    = Priority.MEDIUM
)