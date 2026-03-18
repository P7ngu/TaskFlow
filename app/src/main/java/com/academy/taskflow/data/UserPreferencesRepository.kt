package com.academy.taskflow.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/*
 * UserPreferencesRepository — gestisce le preferenze utente con DataStore.
 *
 * DataStore è il sostituto moderno di SharedPreferences:
 *   - thread-safe: scritture concorrenti non corrompono i dati
 *   - basato su Flow: la UI si aggiorna automaticamente
 *   - atomico: edit { } va a buon fine o non cambia nulla
 *
 * @Singleton: una sola istanza in tutta l'app — Hilt la gestisce.
 * @Inject constructor: Hilt inietta il DataStore automaticamente.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    /*
     * Chiavi tipizzate — un errore nel nome è un errore di compilazione.
     * booleanPreferencesKey: chiave per valori Boolean.
     * stringPreferencesKey, intPreferencesKey ecc. per altri tipi.
     */
    companion object {
        val SHOW_COMPLETED  = booleanPreferencesKey("show_completed")
        val DARK_MODE       = booleanPreferencesKey("dark_mode")
    }

    /*
     * showCompleted: Flow<Boolean> — si aggiorna automaticamente.
     * dataStore.data: Flow<Preferences> emette ad ogni modifica.
     * map: estrae il valore dalla chiave, default false.
     */
    val showCompleted: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[SHOW_COMPLETED] ?: true }

    val isDarkMode: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[DARK_MODE] ?: false }

    /*
     * setShowCompleted: scrive atomicamente.
     * edit { }: transazione — o va a buon fine o non cambia nulla.
     * Chiama da una coroutine (suspend).
     */
    suspend fun setShowCompleted(show: Boolean) {
        dataStore.edit { prefs -> prefs[SHOW_COMPLETED] = show }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DARK_MODE] = enabled }
    }
}
