package com.academy.taskflow.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/*
 * TaskDatabase — il punto di accesso al database Room.
 *
 * @Database: dice a Room che questa è la classe principale del DB.
 *   entities: le tabelle del database — una per ogni @Entity
 *   version:  la versione del DB — incrementare ad ogni modifica dello schema
 *   exportSchema: false nei progetti di esempio — true in produzione
 *
 * RoomDatabase: la classe base fornita da Room.
 * abstract: Room genera l'implementazione concreta a compile-time.
 *
 * Non creare mai direttamente con il costruttore —
 * usare Room.databaseBuilder() o Hilt (DatabaseModule).
 */
@Database(
    entities      = [TaskEntity::class],
    version       = 1,
    exportSchema  = false
)
abstract class TaskDatabase : RoomDatabase() {

    /*
     * taskDao: il DAO per le operazioni sui task.
     * abstract: Room genera l'implementazione.
     * Hilt inietta il TaskDatabase e chiama questo metodo.
     */
    abstract fun taskDao(): TaskDao
}
