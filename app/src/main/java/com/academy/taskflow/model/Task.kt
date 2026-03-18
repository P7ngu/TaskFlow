package com.academy.taskflow.model


import java.util.UUID


/*
 * Priority — enum class per la priorità del task.
 * Le enum in Kotlin sono classi: possono avere proprietà.
 * label: stringa mostrata nella UI ("Alta", "Media", "Bassa")
 * color: colore ARGB come Long — 0xFF = completamente opaco
 */
enum class Priority(val label: String, val color: Long) {
    LOW   ("Bassa",  0xFF4C4F50L),  /* grigio scuro */
    MEDIUM("Media",  0xFFFFC107L),  /* giallo */
    HIGH  ("Alta",   0xFFF44336L)   /* rosso */
}


/*
 * Task — il domain model principale dell'app.
 *
 * data class: il compilatore genera automaticamente
 *   equals()   → confronto per valore
 *   hashCode() → usato in Map e Set
 *   copy()     → crea copia con campi modificati
 *
 * Tutti val → immutabile per design.
 * Per modificare un task si usa copy() — mai si tocca l'originale.
 */
data class Task(
    /*
     * id: UUID garantisce unicità anche con titoli identici.
     * UUID.randomUUID().toString() genera un ID unico ogni volta.
     */
    val id          : String   = UUID.randomUUID().toString(),
    /* title: obbligatorio — nessun default */
    val title       : String,
    /* description: opzionale — stringa vuota come default */
    val description : String   = "",
    /* priority: default MEDIUM — la scelta più comune */
    val priority    : Priority = Priority.MEDIUM,
    /* isCompleted: false di default — il task nasce non completato */
    val isCompleted : Boolean  = false
) {
    /*
     * isDone: proprietà calcolata — alias leggibile di isCompleted.
     * get(): ricalcolata ad ogni accesso — sempre aggiornata.
     * Usiamo isDone nella UI perché è più espressiva.
     */
    val isDone: Boolean get() = isCompleted
}

