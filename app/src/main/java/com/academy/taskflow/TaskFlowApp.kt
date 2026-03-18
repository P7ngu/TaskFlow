package com.academy.taskflow

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/*
 * TaskFlowApp — Application class con Hilt e WorkManager.
 *
 * @HiltAndroidApp: attiva Hilt per tutta l'app — OBBLIGATORIO.
 *
 * Configuration.Provider: permette a WorkManager di usare Hilt
 * per iniettare dipendenze nei Worker (@HiltWorker).
 * Senza questo, WorkManager non può iniettare TaskRepository nel Worker.
 *
 * HiltWorkerFactory: la factory di Hilt per creare i Worker.
 * @Inject: Hilt la inietta automaticamente.
 */
@HiltAndroidApp
class TaskFlowApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /*
     * workManagerConfiguration: configurazione di WorkManager.
     * setWorkerFactory(workerFactory): usa la factory di Hilt
     * invece di quella di default — permette l'injection nei Worker.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
