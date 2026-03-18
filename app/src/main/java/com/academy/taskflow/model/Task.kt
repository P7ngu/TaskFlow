package com.academy.taskflow.model

import java.util.UUID

/*
 * Priority — enum class per la priorità del task.
 * label: stringa mostrata nella UI
 * color: colore ARGB come Long (0xFF = opaco)
 */
enum class Priority(val label: String, val color: Long) {
    LOW   ("Bassa",  0xFF4C4F50L),
    MEDIUM("Media",  0xFFFFC107L),
    HIGH  ("Alta",   0xFFF44336L)
}

/*
 * Task — il domain model dell'app.
 *
 * Questo è il modello che usa la UI e il ViewModel.
 * NON viene salvato direttamente in Room — esiste TaskEntity per quello.
 * Il mapper converte Task ↔ TaskEntity ai confini del data layer.
 *
 * data class: genera equals(), hashCode(), toString(), copy() automaticamente.
 * Tutti i campi sono val — immutabile per design.
 */
data class Task(
    val id          : String   = UUID.randomUUID().toString(),
    val title       : String,
    val description : String   = "",
    val priority    : Priority = Priority.MEDIUM,
    val isCompleted : Boolean  = false
) {
    /*
     * isDone: alias leggibile di isCompleted.
     * Proprietà calcolata — ricalcolata ad ogni accesso.
     * Usiamo isDone nella UI perché è più espressiva.
     */
    val isDone: Boolean get() = isCompleted
}
