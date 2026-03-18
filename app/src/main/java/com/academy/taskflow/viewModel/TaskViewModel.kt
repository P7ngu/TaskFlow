/*
 * TaskViewModel.kt
 * Strato di presentazione — unico punto di contatto tra UI e dati.
 *
 * Responsabilità:
 *   - esporre lo stato della schermata come StateFlow
 *   - ricevere eventi dalla UI e delegare la logica ai repository
 *   - non contenere mai riferimenti a Context, View o Composable
 *
 * @HiltViewModel: Hilt gestisce il ciclo di vita e le dipendenze.
 * Il ViewModel sopravvive alle rotazioni dello schermo — la UI no.
 */
package com.academy.taskflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.academy.taskflow.data.UserPreferencesRepository
import com.academy.taskflow.di.IoDispatcher
import com.academy.taskflow.domain.TaskRepository
import com.academy.taskflow.model.Priority
import com.academy.taskflow.model.Task
import com.academy.taskflow.work.SyncTasksWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/*
 * TaskUiState — sealed class con tutti gli stati possibili della schermata.
 *
 * sealed class: l'insieme dei sottotipi è chiuso e noto a compile time.
 * Il compilatore può verificare che un `when` li copra tutti —
 * nessun caso dimenticato causa comportamenti imprevisti a runtime.
 *
 * I tre stati modellano il ciclo di vita dei dati:
 *   Loading → i dati non sono ancora arrivati dal database
 *   Success → i dati sono pronti, la lista può essere mostrata
 *   Empty   → i dati sono arrivati ma la lista è vuota
 */
sealed class TaskUiState {
    /* object: istanza singleton — Loading non porta dati aggiuntivi */
    object Loading : TaskUiState()

    /*
     * data class: porta la lista dei task da mostrare.
     * data class genera automaticamente equals/hashCode/copy/toString —
     * Compose usa equals per capire se lo stato è cambiato e
     * decidere se ricomporre o saltare.
     */
    data class Success(val tasks: List<Task>) : TaskUiState()

    /* object: istanza singleton — Empty non porta dati aggiuntivi */
    object Empty : TaskUiState()
}

@HiltViewModel
class TaskViewModel @Inject constructor(
    /*
     * TaskRepository: astrazione sul database Room.
     * Il ViewModel non conosce Room direttamente — dipende
     * dall'interfaccia, non dall'implementazione.
     * Questo rende il ViewModel testabile con un fake repository.
     */
    private val repository      : TaskRepository,

    /*
     * UserPreferencesRepository: astrazione su DataStore.
     * Espone le preferenze utente come Flow — ogni modifica
     * viene propagata automaticamente alla UI.
     */
    private val preferencesRepo : UserPreferencesRepository,

    /*
     * WorkManager: gestione dei task in background.
     * Iniettato per avviare e osservare le sincronizzazioni.
     */
    private val workManager     : WorkManager,

    /*
     * @IoDispatcher: dispatcher coroutine per operazioni I/O.
     * Le operazioni su database e rete non vanno mai sul thread
     * principale (Main) — causerebbero un ANR (Application Not Responding).
     * Il dispatcher IO usa un pool di thread dedicato.
     */
    @IoDispatcher private val dispatcher: CoroutineDispatcher
) : ViewModel() {

    /* ── Stato UI ──────────────────────────────────────────────────────── */

    /*
     * _uiState: MutableStateFlow privato — solo il ViewModel scrive.
     * Il valore iniziale è Loading — l'UI mostra uno spinner
     * finché i dati non arrivano dal database.
     *
     * Convenzione Kotlin: il prefisso _ indica un backing field privato.
     */
    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)

    /*
     * uiState: StateFlow pubblico — la UI legge, non scrive.
     * asStateFlow(): converte il MutableStateFlow in uno StateFlow
     * read-only — impedisce che la UI modifichi lo stato direttamente.
     */
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    /* ── Snackbar ──────────────────────────────────────────────────────── */

    /*
     * SnackbarEvent — dato che viaggia dal ViewModel alla UI.
     * Contiene l'id del task (per annullare l'eliminazione)
     * e il titolo (da mostrare nel messaggio della Snackbar).
     * È una data class perché trasporta dati — non è un singleton.
     *
     * PERCHÉ è dentro la classe:
     * È un dettaglio implementativo del ViewModel — non ha senso
     * che esista come tipo globale fuori dallo scope del ViewModel.
     */
    data class SnackbarEvent(val taskId: String, val taskTitle: String)

    /*
     * _snackbarEvent: Channel per gli eventi one-shot verso la UI.
     *
     * PERCHÉ Channel e non StateFlow:
     * StateFlow mantiene l'ultimo valore e lo riemette ad ogni
     * nuovo osservatore — sbagliato per gli eventi: se l'utente
     * ruota lo schermo, la Snackbar verrebbe mostrata di nuovo.
     * Channel è "consumabile" — ogni evento viene ricevuto
     * una sola volta dal primo collettore disponibile.
     *
     * Channel.BUFFERED: se la UI non sta ancora raccogliendo,
     * l'evento viene messo in coda invece di essere perso.
     *
     * PERCHÉ è dentro la classe:
     * Usa viewModelScope implicitamente tramite send() —
     * deve vivere nello stesso scope del ViewModel.
     */
    private val _snackbarEvent = Channel<SnackbarEvent>(Channel.BUFFERED)

    /*
     * snackbarEvent: Flow pubblico derivato dal Channel.
     * receiveAsFlow(): converte il Channel in un Flow leggibile
     * dalla UI — non espone il Channel mutabile direttamente.
     */
    val snackbarEvent = _snackbarEvent.receiveAsFlow()

    /*
     * pendingDeletions: mappa che tiene traccia dei Job di eliminazione
     * in attesa, indicizzati per taskId.
     *
     * Ogni volta che un task viene completato, parte un Job con delay
     * di 3 secondi. La mappa permette di cancellare quel Job specifico
     * se l'utente preme "Annulla" prima che scada il timer.
     *
     * MutableMap: aggiornata dal ViewModel stesso — non esposta alla UI.
     *
     * PERCHÉ è dentro la classe:
     * Contiene riferimenti a Job figli di viewModelScope —
     * devono essere cancellati quando il ViewModel viene distrutto.
     */
    private val pendingDeletions = mutableMapOf<String, Job>()

    /* ── Preferenze ────────────────────────────────────────────────────── */

    /*
     * showCompleted: preferenza "mostra task completati".
     * stateIn: converte il Flow di DataStore in uno StateFlow.
     *   viewModelScope: il Flow si ferma quando il ViewModel viene distrutto.
     *   WhileSubscribed(5000): il Flow si ferma 5 secondi dopo che
     *     l'ultimo osservatore si disconnette — evita sprechi quando
     *     l'app va in background, ma mantiene il valore in cache
     *     per un ritorno rapido in foreground.
     *   true: valore iniziale prima che DataStore emetta il primo valore.
     */
    val showCompleted: StateFlow<Boolean> = preferencesRepo.showCompleted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    /*
     * isDarkMode: preferenza tema scuro.
     * Stesso pattern di showCompleted — valore iniziale false (tema chiaro).
     */
    val isDarkMode: StateFlow<Boolean> = preferencesRepo.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    /* ── Inizializzazione ──────────────────────────────────────────────── */

    /*
     * init: blocco eseguito una sola volta alla creazione del ViewModel.
     * loadTasks(): inizia ad osservare il database.
     * seedDatabaseIfEmpty(): popola il database con dati iniziali
     *   se è la prima apertura dell'app.
     */
    init {
        loadTasks()
        seedDatabaseIfEmpty()
    }

    /* ── Logica interna ────────────────────────────────────────────────── */

    /*
     * loadTasks: osserva il database e la preferenza filtro in combinazione.
     *
     * combine: operatore che fonde due Flow in uno solo.
     * Emette un nuovo valore ogni volta che UNO dei due Flow cambia.
     * Esempio: l'utente attiva "nascondi completati" → showCompleted
     * emette false → combine ricalcola → _uiState si aggiorna → UI si
     * ricompone con la lista filtrata. Tutto automatico, zero codice
     * imperativo.
     *
     * onEach: effetto collaterale per ogni emissione — aggiorna _uiState.
     * launchIn: avvia la raccolta del Flow nello scope del ViewModel.
     * Quando il ViewModel viene distrutto, viewModelScope viene
     * cancellato e il Flow smette di essere osservato.
     */
    private fun loadTasks() {
        combine(
            repository.getTasksStream(),
            preferencesRepo.showCompleted
        ) { tasks, showCompleted ->
            val filtered = if (showCompleted) tasks
            else tasks.filter { !it.isCompleted }
            if (filtered.isEmpty()) TaskUiState.Empty
            else                    TaskUiState.Success(filtered)
        }
            .onEach  { _uiState.value = it }
            .launchIn(viewModelScope)
    }

    /*
     * seedDatabaseIfEmpty: popola il database con task di esempio
     * se è vuoto — utile per la demo in aula.
     *
     * viewModelScope.launch(dispatcher): avvia una coroutine sul
     * thread IO — le operazioni Room non vanno mai sul Main thread.
     * repository.getTasksStream().first(): legge il primo valore
     * emesso dal Flow e si ferma subito (non osserva continuamente).
     * forEach { repository.saveTask(it) }: salva ogni task nel database.
     */
    private fun seedDatabaseIfEmpty() {
        viewModelScope.launch(dispatcher) {
            repository.getTasksStream().first().let { tasks ->
                if (tasks.isEmpty()) {
                    listOf(
                        Task(title = "Setup Android Studio", priority = Priority.HIGH),
                        Task(title = "Imparare Compose",     priority = Priority.HIGH),
                        Task(title = "Integrare Room",       priority = Priority.MEDIUM),
                        Task(title = "Configurare Hilt",     priority = Priority.MEDIUM),
                        Task(title = "Scrivere i test",      priority = Priority.LOW)
                    ).forEach { repository.saveTask(it) }
                }
            }
        }
    }

    /* ── Azioni pubbliche ──────────────────────────────────────────────── */

    /*
     * toggleDone: inverte lo stato completato/non completato di un task.
     *
     * Se il task viene segnato come completato:
     *   1. Lo aggiorna in Room tramite repository.
     *   2. Emette un SnackbarEvent per avvisare la UI.
     *   3. Avvia un Job che aspetta 3 secondi e poi elimina il task.
     *
     * Se il task viene ri-segnato come non completato (toggle inverso):
     *   1. Lo aggiorna in Room.
     *   2. Cancella l'eventuale Job di eliminazione in attesa.
     *
     * PERCHÉ si legge lo stato PRIMA di chiamare repository.toggleDone():
     * dopo il toggle, Room aggiorna il Flow in modo asincrono.
     * Leggere lo stato prima garantisce di sapere con certezza
     * in quale direzione sta andando il toggle, senza race condition.
     */
    fun toggleDone(taskId: String) {

        /*
         * Si legge il task dallo stato corrente PRIMA di aggiornare Room.
         * Se non esiste (caso teorico), si esce subito con return.
         */
        val currentTask = getTaskById(taskId) ?: return

        /*
         * willBeCompleted: true se dopo il toggle il task sarà completato.
         * Calcolato prima del toggle — evita race condition con Room.
         */
        val willBeCompleted = !currentTask.isCompleted

        viewModelScope.launch(dispatcher) {

            /* Aggiorna lo stato in Room */
            repository.toggleDone(taskId)

            if (willBeCompleted) {

                /*
                 * Il task è appena stato completato.
                 * Si emette l'evento Snackbar — la UI mostrerà il messaggio
                 * con il titolo del task e il bottone "Annulla".
                 * send(): sospende se il Channel è pieno (quasi mai).
                 */
                _snackbarEvent.send(SnackbarEvent(taskId, currentTask.title))

                /*
                 * Si avvia il Job di eliminazione ritardata.
                 * viewModelScope.launch: nuovo Job figlio dello scope del ViewModel.
                 * Se il ViewModel viene distrutto, tutti i Job figli
                 * vengono cancellati automaticamente — nessun memory leak.
                 */
                val deletionJob = viewModelScope.launch(dispatcher) {

                    /*
                     * delay(3000L): sospende il Job per 3 secondi.
                     * Durante questo tempo l'utente può premere "Annulla".
                     * Se il Job viene cancellato (undoDelete), delay lancia
                     * CancellationException — il codice dopo non viene eseguito.
                     */
                    delay(3000L)

                    /*
                     * isActive: true se il Job non è stato cancellato.
                     * Controllo difensivo — dopo delay il Job potrebbe
                     * essere stato cancellato nell'istante precedente.
                     */
                    if (isActive) {
                        repository.deleteTask(taskId)
                        pendingDeletions.remove(taskId)
                    }
                }

                /*
                 * Si salva il Job nella mappa — servirà per cancellarlo
                 * se l'utente preme "Annulla" entro i 3 secondi.
                 */
                pendingDeletions[taskId] = deletionJob

            } else {

                /*
                 * Il task è stato ri-segnato come non completato.
                 * Se c'era un Job di eliminazione in attesa, lo si cancella —
                 * l'utente ha cambiato idea prima che scadesse il timer.
                 */
                pendingDeletions[taskId]?.cancel()
                pendingDeletions.remove(taskId)
            }
        }
    }

    /*
     * undoDelete: annulla l'eliminazione programmata di un task.
     * Chiamato dalla UI quando l'utente preme "Annulla" nella Snackbar.
     *
     * 1. Cancella il Job di eliminazione — delay viene interrotto,
     *    deleteTask non viene mai chiamato.
     * 2. Segna il task come non completato in Room —
     *    torna allo stato precedente il toggle.
     */
    fun undoDelete(taskId: String) {

        /*
         * cancel(): lancia CancellationException nel Job,
         * interrompendo il delay e impedendo l'eliminazione.
         */
        pendingDeletions[taskId]?.cancel()
        pendingDeletions.remove(taskId)

        /*
         * Si chiama repository.toggleDone() direttamente
         * invece di chiamare this.toggleDone() — per evitare
         * di riavviare un altro ciclo di eliminazione ritardata.
         */
        viewModelScope.launch(dispatcher) {
            repository.toggleDone(taskId)
        }
    }

    /*
     * getTaskById: cerca un task per id nello stato corrente.
     *
     * Usato come funzione di utilità interna — non sostituisce
     * l'osservazione reattiva dello stato.
     * Restituisce null se lo stato non è Success o se il task
     * non esiste nella lista.
     */
    fun getTaskById(taskId: String): Task? {
        val current = _uiState.value as? TaskUiState.Success ?: return null
        return current.tasks.firstOrNull { it.id == taskId }
    }

    /* ── DataStore ─────────────────────────────────────────────────────── */

    /*
     * toggleShowCompleted: inverte la preferenza "mostra completati".
     *
     * La catena reattiva si aggiorna da sola:
     * DataStore scrive → showCompleted emette → combine in loadTasks
     * riceve il nuovo valore → _uiState si aggiorna → UI si ricompone.
     */
    fun toggleShowCompleted() {
        viewModelScope.launch {
            preferencesRepo.setShowCompleted(!showCompleted.value)
        }
    }

    /*
     * toggleDarkMode: inverte la preferenza tema scuro.
     * Stesso pattern di toggleShowCompleted.
     */
    fun toggleDarkMode() {
        viewModelScope.launch {
            preferencesRepo.setDarkMode(!isDarkMode.value)
        }
    }

    /* ── WorkManager ───────────────────────────────────────────────────── */

    /*
     * syncNow: avvia una sincronizzazione immediata.
     *
     * OneTimeWorkRequestBuilder: job singolo, non periodico.
     * Constraints.CONNECTED: il job parte solo con rete disponibile —
     * se non c'è rete WorkManager lo mette in coda e lo esegue
     * non appena la connessione si ripristina.
     * enqueueUniqueWork con KEEP: se un sync è già in corso
     * non ne avvia un secondo — evita richieste duplicate.
     */
    fun syncNow() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncTasksWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            SyncTasksWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    /*
     * schedulePeriodicalSync: pianifica una sincronizzazione periodica.
     *
     * PeriodicWorkRequestBuilder: si ripete ogni 6 ore.
     * Il sistema non garantisce l'esecuzione esatta — può variare
     * di qualche minuto per ottimizzare batteria e rete.
     * Il minimo di sistema è 15 minuti — valori inferiori vengono ignorati.
     * UNMETERED: solo su WiFi — evita consumo di dati mobili.
     * setRequiresBatteryNotLow: non parte se la batteria è scarica.
     * UPDATE: se esiste già una richiesta periodica la aggiorna
     * invece di aggiungerne una duplicata.
     */
    fun schedulePeriodicalSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SyncTasksWorker>(
            6, TimeUnit.HOURS
        ).setConstraints(constraints).build()

        workManager.enqueueUniquePeriodicWork(
            "${SyncTasksWorker.WORK_NAME}_periodic",
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    /*
     * syncState: Flow che osserva lo stato del worker di sincronizzazione.
     *
     * getWorkInfosForUniqueWorkFlow: restituisce un Flow<List<WorkInfo>>
     * che emette ogni volta che lo stato del job cambia.
     * map: estrae solo lo stato (ENQUEUED, RUNNING, SUCCEEDED, FAILED, ecc.)
     * del primo job nella lista — ce n'è sempre al massimo uno
     * perché usiamo enqueueUniqueWork.
     * La UI usa questo StateFlow per colorare l'icona di sincronizzazione
     * e mostrare la barra di progresso.
     */
    val syncState: StateFlow<WorkInfo.State?> =
        workManager.getWorkInfosForUniqueWorkFlow(SyncTasksWorker.WORK_NAME)
            .map    { infos -> infos.firstOrNull()?.state }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /* ── Aggiunta task ─────────────────────────────────────────────────── */

    /*
     * addTask: crea un nuovo Task con i dati inseriti dall'utente
     * e lo salva nel database tramite repository.
     *
     * PERCHÉ repository.saveTask() e non _uiState.value = ... :
     * l'app usa Room come sorgente di verità (single source of truth).
     * loadTasks() osserva già getTasksStream() — quando Room cambia,
     * il Flow emette e _uiState si aggiorna da solo.
     * Modificare _uiState direttamente bypasserebbe Room e causerebbe
     * desincronizzazione tra UI e database.
     *
     * UUID.randomUUID(): genera un identificatore unico universale —
     * nessun rischio di collisione anche con molti task in lista.
     *
     * viewModelScope.launch(dispatcher): operazione su thread IO —
     * Room non può essere chiamato sul Main thread.
     */
    fun addTask(
        title       : String,
        description : String,
        priority    : Priority,
        dueDate     : LocalDate?   // ignorato per ora — Task non ha ancora questo campo
    ) {
        viewModelScope.launch(dispatcher) {
            val newTask = Task(
                id          = UUID.randomUUID().toString(),
                title       = title,
                description = description,
                priority    = priority
                /*
                 * dueDate non viene passato perché la data class Task
                 * non ha ancora questo campo. Per aggiungerlo:
                 * 1. aggiungere `val dueDate: LocalDate? = null` a Task
                 * 2. aggiungere la colonna alla Room Entity con @ColumnInfo
                 * 3. incrementare il version number del database
                 *    e aggiungere una Migration (o allowDestructiveMigration
                 *    in fase di sviluppo).
                 */
            )
            repository.saveTask(newTask)
        }
    }

} // ← parentesi graffa di chiusura della classe — tutto il codice è dentro