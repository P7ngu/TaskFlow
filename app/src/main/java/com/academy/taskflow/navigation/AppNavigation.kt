/*
 * AppNavigation.kt
 * Contiene il grafo di navigazione completo dell'app.
 * È l'unico punto dove si definiscono destinazioni e transizioni —
 * centralizzare la navigazione evita che le schermate si conoscano
 * tra loro e mantiene il codice manutenibile.
 */
package com.academy.taskflow.navigation

/* Composable: annotation che marca una funzione come componente UI Compose */
import androidx.compose.runtime.Composable
/* getValue: operatore delegato — permette `by` su State e Flow */
import androidx.compose.runtime.getValue
/* LaunchedEffect: coroutine legata al ciclo di vita del Composable */
import androidx.compose.runtime.LaunchedEffect
/* remember: mantiene un valore in memoria attraverso le ricomposizioni */
import androidx.compose.runtime.remember
/* SnackbarDuration: enum con le durate disponibili per la Snackbar */
import androidx.compose.material3.SnackbarDuration
/* SnackbarHostState: stato che gestisce la coda e le animazioni della Snackbar */
import androidx.compose.material3.SnackbarHostState
/* SnackbarResult: risultato dell'interazione dell'utente con la Snackbar */
import androidx.compose.material3.SnackbarResult
/* hiltViewModel: crea o recupera un ViewModel iniettato da Hilt */
import androidx.hilt.navigation.compose.hiltViewModel
/* collectAsStateWithLifecycle: converte un Flow in State rispettando il lifecycle */
import androidx.lifecycle.compose.collectAsStateWithLifecycle
/* NavType: descrive il tipo di un argomento di navigazione (String, Int, ecc.) */
import androidx.navigation.NavType
/* NavHost: il contenitore del grafo — mostra la destinazione attiva */
import androidx.navigation.compose.NavHost
/* composable: DSL per dichiarare una destinazione nel NavHost */
import androidx.navigation.compose.composable
/* rememberNavController: crea e memorizza il controller di navigazione */
import androidx.navigation.compose.rememberNavController
/* navArgument: dichiara un parametro tipizzato nel path di una route */
import androidx.navigation.navArgument
import com.academy.taskflow.ui.permissions.PermissionHandler
import com.academy.taskflow.ui.tasks.AddTaskScreen
import com.academy.taskflow.ui.tasks.TaskDetailScreen
import com.academy.taskflow.ui.tasks.TaskListScreen
import com.academy.taskflow.viewmodel.TaskUiState
import com.academy.taskflow.viewmodel.TaskViewModel

/*
 * Screen — sealed class che centralizza tutti i nomi delle route.
 *
 * Una route è una stringa che identifica una destinazione nel grafo.
 * Usare stringhe libere nel codice è pericoloso: un typo causa un crash
 * a runtime, non un errore di compilazione.
 * La sealed class sposta l'errore a compile time — il compilatore
 * segnala subito se si usa un nome inesistente.
 *
 * sealed class: tutte le sottoclassi devono essere nello stesso file.
 * Questo garantisce che l'insieme delle route sia chiuso e noto.
 */
sealed class Screen(val route: String) {

    /*
     * TaskList: la schermata principale con la lista dei task.
     * Nessun parametro nel path — la route è una stringa semplice.
     */
    object TaskList : Screen("task_list")

    /*
     * TaskDetail: la schermata di dettaglio di un singolo task.
     * {taskId} è un segnaposto nel path — viene sostituito con
     * il valore reale al momento della navigazione.
     * Esempio: "task_detail/abc123"
     */
    object TaskDetail : Screen("task_detail/{taskId}") {
        /*
         * createRoute: costruisce la route con l'id reale.
         * Si usa invece di concatenare stringhe a mano —
         * più leggibile e meno soggetto a errori.
         */
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }

    /*
     * AddTask: la schermata di creazione di un nuovo task.
     * Nessun parametro nel path — i dati vengono raccolti nel form
     * e restituiti al chiamante tramite la lambda onSave.
     */
    object AddTask : Screen("add_task")
}

/*
 * AppNavigation — il grafo di navigazione completo dell'app.
 *
 * È un Composable senza UI propria: si occupa solo di creare
 * il NavController, osservare lo stato del ViewModel e collegare
 * le schermate tra loro.
 *
 * Il ViewModel è istanziato qui e condiviso tra tutte le schermate —
 * così tutte lavorano sugli stessi dati senza doversi passare
 * riferimenti l'una con l'altra.
 */
@Composable
fun AppNavigation() {

    /*
     * rememberNavController: crea il NavController e lo mantiene
     * in memoria attraverso le ricomposizioni.
     * Il NavController gestisce il back stack — la sequenza di
     * schermate visitate — e permette di navigare avanti e indietro.
     */
    val navController = rememberNavController()

    /*
     * hiltViewModel: recupera il ViewModel dallo scope Hilt.
     * Se il ViewModel esiste già (es. dopo una rotazione dello schermo)
     * viene restituito quello esistente — non ne viene creato uno nuovo.
     * Hilt si occupa di iniettare le dipendenze dichiarate nel costruttore.
     */
    val viewModel: TaskViewModel = hiltViewModel()

    /*
     * collectAsStateWithLifecycle: osserva il Flow del ViewModel
     * e lo converte in uno State<T> che Compose sa leggere.
     * "WithLifecycle": smette di osservare quando l'app va in background
     * e riprende quando torna in foreground — risparmia risorse.
     * `by`: delega — la variabile ha il tipo T, non State<T>.
     */
    val uiState       by viewModel.uiState.collectAsStateWithLifecycle()
    val showCompleted by viewModel.showCompleted.collectAsStateWithLifecycle()
    val syncState     by viewModel.syncState.collectAsStateWithLifecycle()

    /*
     * PermissionHandler: Composable senza UI.
     * Richiede i permessi runtime (notifiche, calendario, allarmi)
     * al primo avvio dell'app. Va chiamato qui, fuori dal NavHost,
     * così viene eseguito una sola volta qualunque sia la schermata attiva.
     */
    PermissionHandler()

    /*
     * NavHost: il contenitore del grafo di navigazione.
     * Mostra sempre e solo la destinazione attiva nel back stack.
     * navController: il controller che gestisce le transizioni.
     * startDestination: la schermata mostrata al primo avvio.
     */
    NavHost(
        navController    = navController,
        startDestination = Screen.TaskList.route
    ) {

        /* ── SCHERMATA LISTA TASK ────────────────────────────────────── */
        composable(Screen.TaskList.route) {

            /*
             * snackbarHostState: stato condiviso tra AppNavigation
             * e TaskListScreen.
             * remember: viene creato una sola volta e mantenuto
             * attraverso le ricomposizioni — non viene ricreato
             * ad ogni render.
             * Gestisce la coda delle Snackbar e le loro animazioni.
             */
            val snackbarHostState = remember { SnackbarHostState() }

            /*
             * LaunchedEffect(Unit): coroutine avviata una sola volta
             * al primo avvio di questo composable.
             * Unit come chiave: garantisce che non venga rieseguita
             * ad ogni ricomposizione.
             * Rimane attiva finché il composable è nel back stack —
             * raccoglie tutti gli eventi Snackbar emessi dal ViewModel.
             *
             * collect: sospende in attesa del prossimo evento.
             * Ogni evento sblocca il collect, mostra la Snackbar
             * e poi riprende ad aspettare il prossimo.
             */
            LaunchedEffect(Unit) {
                viewModel.snackbarEvent.collect { event ->

                    /*
                     * showSnackbar: mostra la Snackbar e sospende
                     * finché l'utente non interagisce o scade il timer.
                     * Restituisce SnackbarResult:
                     *   ActionPerformed → l'utente ha premuto "Annulla"
                     *   Dismissed       → la Snackbar è scaduta o è stata
                     *                     sostituita da un'altra
                     */
                    val result = snackbarHostState.showSnackbar(
                        message     = "\"${event.taskTitle}\" completato — verrà eliminato",
                        actionLabel = "Annulla",
                        /*
                         * SnackbarDuration.Short: circa 4 secondi —
                         * leggermente più dei 3 secondi del timer di
                         * eliminazione, così l'utente ha sempre il tempo
                         * di premere "Annulla" prima che il task sparisca.
                         */
                        duration    = SnackbarDuration.Short
                    )

                    /*
                     * Se l'utente ha premuto "Annulla" entro il tempo
                     * limite, si chiama undoDelete — il Job di eliminazione
                     * viene cancellato e il task torna come non completato.
                     */
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undoDelete(event.taskId)
                    }
                }
            }

            TaskListScreen(
                uiState        = uiState,
                /*
                 * onTaskClick: naviga al dettaglio passando l'id nel path.
                 * La route viene costruita con createRoute per evitare
                 * concatenazioni manuali di stringhe.
                 */
                onTaskClick    = { id ->
                    navController.navigate(Screen.TaskDetail.createRoute(id))
                },
                /* onToggleDone: delega al ViewModel la logica di toggle */
                onToggleDone   = { id -> viewModel.toggleDone(id) },
                showCompleted  = showCompleted,
                onToggleFilter = { viewModel.toggleShowCompleted() },
                syncState      = syncState,
                onSyncNow      = { viewModel.syncNow() },
                /* onAddTask: naviga alla schermata di creazione task */
                onAddTask      = { navController.navigate(Screen.AddTask.route) },
                /*
                 * snackbarHostState: passato alla schermata così lo Scaffold
                 * interno può mostrare le Snackbar nella posizione corretta
                 * rispetto al FAB e alla TopAppBar.
                 */
                snackbarHostState = snackbarHostState
            )
        }

        /* ── SCHERMATA DETTAGLIO TASK ────────────────────────────────── */

        /*
         * composable con argomenti: la route contiene {taskId},
         * quindi va dichiarato il tipo del parametro con navArgument.
         * Senza questa dichiarazione, Navigation non sa come
         * estrarre il valore dalla stringa della route.
         */
        composable(
            route     = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType })
        ) { backStack ->

            /*
             * backStack.arguments: la Bundle con i valori dei parametri.
             * getString("taskId"): estrae il valore del parametro dal path.
             * ?: "": fallback — se null (non dovrebbe mai accadere)
             * si usa stringa vuota per evitare un crash NullPointerException.
             */
            val taskId = backStack.arguments?.getString("taskId") ?: ""

            /*
             * Il task viene derivato da uiState, non da getTaskById().
             *
             * PERCHÉ: uiState è uno State<T> osservato da Compose —
             * ogni volta che cambia (es. dopo toggleDone), Compose
             * ricalcola questa riga e ricompone TaskDetailScreen
             * con il valore aggiornato.
             *
             * getTaskById() è una funzione one-shot: legge una volta
             * e non reagisce ai cambiamenti successivi. Usarla qui
             * causerebbe il bug del bottone che non aggiorna la UI.
             *
             * as?: safe cast — se uiState non è Success restituisce null.
             * ?.tasks: accede alla lista solo se il cast è riuscito.
             * find: cerca il task con l'id corrispondente.
             */
            val task = (uiState as? TaskUiState.Success)
                ?.tasks
                ?.find { it.id == taskId }

            /*
             * Guardia null: si renderizza TaskDetailScreen solo se
             * il task esiste nella lista. Il caso null si verifica
             * se il task è stato eliminato mentre si era nel dettaglio —
             * in quel caso la schermata rimane vuota e l'utente
             * può tornare indietro con il tasto Back.
             */
            if (task != null) {
                TaskDetailScreen(
                    task    = task,
                    /* onBack: rimuove la destinazione corrente dal back stack */
                    onBack  = { navController.popBackStack() },
                    /*
                     * onToggleDone: delega al ViewModel.
                     * Il ViewModel aggiorna lo StateFlow → uiState cambia
                     * → `task` sopra viene ricalcolato → la UI si ricompone
                     * automaticamente con isDone aggiornato.
                     */
                    onToggleDone = { viewModel.toggleDone(taskId) }
                )
            }
        }

        /* ── SCHERMATA AGGIUNTA TASK ─────────────────────────────────── */

        /*
         * Nessun argomento nel path — i dati viaggiano tramite lambda.
         * onBack: annulla la creazione e torna alla lista.
         * onSave: riceve i valori compilati dall'utente, li passa
         * al ViewModel per creare il task, poi torna alla lista.
         */
        composable(Screen.AddTask.route) {
            AddTaskScreen(
                onBack = { navController.popBackStack() },
                onSave = { title, description, priority, dueDate ->
                    /*
                     * viewModel.addTask: salva il nuovo task in Room.
                     * Il Flow di getTasksStream() emette automaticamente
                     * il nuovo valore → la lista si aggiorna senza
                     * nessun codice aggiuntivo.
                     */
                    viewModel.addTask(title, description, priority, dueDate)
                    /*
                     * popBackStack: torna alla lista dopo il salvataggio.
                     * L'utente vede immediatamente il nuovo task in lista.
                     */
                    navController.popBackStack()
                }
            )
        }
    }
}