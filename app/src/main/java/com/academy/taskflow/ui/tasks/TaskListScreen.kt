package com.academy.taskflow.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import com.academy.taskflow.viewmodel.TaskUiState

/*
 * TaskListScreen — schermata principale con la lista dei task.
 * Stateless — pattern state down, events up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    uiState           : TaskUiState,
    onTaskClick       : (String) -> Unit,
    onToggleDone      : (String) -> Unit,
    showCompleted     : Boolean          = true,
    onToggleFilter    : () -> Unit       = {},
    syncState         : WorkInfo.State?  = null,
    onSyncNow         : () -> Unit       = {},
    onAddTask         : () -> Unit,
    /*
     * snackbarHostState: stato della Snackbar passato dall'esterno.
     * Non viene creato qui dentro — è condiviso con AppNavigation
     * che gestisce la logica di show/dismiss tramite LaunchedEffect.
     * Separare stato e UI permette di controllare le Snackbar
     * da fuori della schermata.
     */
    snackbarHostState : SnackbarHostState
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title  = { Text("TaskFlow") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    /*
                     * IconButton filtro — contentDescription obbligatoria.
                     * WCAG 1.1.1: le icone interattive devono avere
                     * alternativa testuale. Un'icona senza descrizione
                     * è inaccessibile agli utenti TalkBack.
                     *
                     * La descrizione cambia dinamicamente con lo stato —
                     * TalkBack annuncia lo stato corrente e l'azione disponibile.
                     */
                    IconButton(
                        onClick = onToggleFilter,
                        modifier = Modifier.semantics {
                            contentDescription = if (showCompleted)
                                "Nascondi task completati"
                                else "Mostra task completati"
                        }
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Done,
                            /*
                             * contentDescription = null sull'Icon interno:
                             * il contentDescription è già sul padre (IconButton).
                             * mergeDescendants = true (implicito in IconButton)
                             * unifica la semantica — evita duplicazione.
                             */
                            contentDescription = null,
                            tint = if (showCompleted)
                                MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(
                        onClick = onSyncNow,
                        modifier = Modifier.semantics {
                            contentDescription = when (syncState) {
                                WorkInfo.State.RUNNING -> "Sincronizzazione in corso"
                                WorkInfo.State.FAILED  -> "Sincronizzazione fallita — riprova"
                                else                   -> "Sincronizza task dalla rete"
                            }
                        }
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = when (syncState) {
                                WorkInfo.State.RUNNING -> MaterialTheme.colorScheme.primary
                                WorkInfo.State.FAILED  -> MaterialTheme.colorScheme.error
                                else                   -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            /*
             * FloatingActionButton: il punto di accesso principale
             * all'azione primaria della schermata — in questo caso
             * aprire il form di creazione task.
             * È posizionato in basso a destra di default da Scaffold.
             */
            FloatingActionButton(onClick = onAddTask) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Aggiungi task"
                )
            }
        },

        /*
    * snackbarHost: il punto dello schermo dove le Snackbar vengono
    * mostrate. Scaffold lo posiziona automaticamente sopra il FAB
    * e rispetta i padding della navigation bar di sistema.
    * SnackbarHost(snackbarHostState): collega lo stato al widget
    * che gestisce le animazioni di entrata e uscita.
    */
        snackbarHost = { SnackbarHost(snackbarHostState) }

    )
    { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            if (syncState == WorkInfo.State.RUNNING) {
                /*
                 * LinearProgressIndicator con semantics.
                 * WCAG 4.1.3 Status Messages: i messaggi di stato
                 * devono essere annunciati agli screen reader
                 * senza spostare il focus dell'utente.
                 * liveRegion = LiveRegionMode.Polite: TalkBack annuncia
                 * la presenza dell'indicatore al termine della lettura corrente.
                 */
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .semantics {
                            contentDescription = "Sincronizzazione in corso"
                            liveRegion = LiveRegionMode.Polite
                        }
                )
            }
            when (uiState) {
                is TaskUiState.Loading ->
                    /*
                     * CircularProgressIndicator con contentDescription.
                     * Senza questa, TalkBack non annuncia nulla —
                     * l'utente non sa se l'app sta caricando o è bloccata.
                     */
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .semantics {
                                contentDescription = "Caricamento task in corso"
                            }
                    )
                is TaskUiState.Success ->
                    LazyColumn(
                        contentPadding      = PaddingValues(top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.tasks, key = { it.id }) { task ->
                            TaskCard(
                                task         = task,
                                onCardClick  = { onTaskClick(task.id) },
                                onToggleDone = { onToggleDone(task.id) }
                            )
                        }
                    }
                is TaskUiState.Empty ->
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Nessun task")
                        if (!showCompleted) {
                            Text(
                                "Stai nascondendo i task completati",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
            }
        }
    }
}
