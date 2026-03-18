package com.academy.taskflow.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.academy.taskflow.model.Priority
import com.academy.taskflow.model.Task

/*
 * TaskEntity — la rappresentazione di Task nel database Room.
 *
 * @Entity: indica a Room che questa classe mappa su una tabella SQLite.
 * tableName: il nome della tabella nel database.
 *
 * Differenza rispetto a Task (domain model):
 *   - TaskEntity vive nel data layer — conosce Room
 *   - Task vive nel domain layer — non sa niente del database
 *   - Il mapper converte tra i due ai confini del layer
 *
 * priority salvato come String perché SQLite non conosce le enum.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id          : String,
    val title       : String,
    val description : String,
    /* priority.name converte l'enum in stringa: HIGH, MEDIUM, LOW */
    val priority    : String,
    val isCompleted : Boolean
)

/*
 * Mapper: TaskEntity → Task (domain model)
 * Extension function: si chiama come metodo su TaskEntity.
 * Priority.valueOf: converte la stringa "HIGH" nell'enum Priority.HIGH
 */
fun TaskEntity.toDomain() = Task(
    id          = id,
    title       = title,
    description = description,
    priority    = Priority.valueOf(priority),
    isCompleted = isCompleted
)

/*
 * Mapper inverso: Task → TaskEntity (per salvare nel DB)
 * priority.name: converte l'enum Priority.HIGH nella stringa "HIGH"
 */
fun Task.toEntity() = TaskEntity(
    id          = id,
    title       = title,
    description = description,
    priority    = priority.name,
    isCompleted = isCompleted
)
