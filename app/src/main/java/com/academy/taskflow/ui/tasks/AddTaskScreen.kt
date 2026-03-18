package com.academy.taskflow.ui.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.academy.taskflow.model.Priority
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material.icons.filled.DateRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    onBack: () -> Unit,
    onSave: (
        title:       String,
        description: String,
        priority:    Priority,
        dueDate:     LocalDate?
    ) -> Unit
) {
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority    by remember { mutableStateOf(Priority.MEDIUM) }
    var titleError  by remember { mutableStateOf(false) }

    /*
     * selectedDate: la data scelta dall'utente tramite il DatePicker.
     * Null finché l'utente non seleziona una data.
     * Separato dal testo — non ha senso parsare una stringa
     * quando il DatePicker restituisce già un valore Long/LocalDate.
     */
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    /*
     * showDatePicker: controlla la visibilità del DatePickerDialog.
     * true = il calendario è aperto.
     * false = il calendario è chiuso (stato iniziale).
     */
    var showDatePicker by remember { mutableStateOf(false) }

    /*
     * datePickerState: stato interno del DatePicker Material 3.
     * Tiene traccia della data selezionata, del mese visualizzato, ecc.
     * rememberDatePickerState: crea lo stato e lo mantiene attraverso
     * le ricomposizioni — non viene ricreato ad ogni render.
     */
    val datePickerState = rememberDatePickerState()

    /*
     * DatePickerDialog: dialog con calendario nativo Material 3.
     * Mostrato solo quando showDatePicker è true.
     * onDismissRequest: chiude il dialog se l'utente tocca fuori.
     * confirmButton / dismissButton: i bottoni di conferma e annulla.
     */
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    /*
                     * datePickerState.selectedDateMillis: la data selezionata
                     * in millisecondi da epoch (1 gennaio 1970).
                     * È null se l'utente non ha ancora selezionato nulla.
                     *
                     * LocalDate.ofEpochDay: converte i millisecondi in LocalDate.
                     * / 86400000L: divide per i millisecondi in un giorno
                     * per ottenere i giorni da epoch.
                     */
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / 86400000L)
                    }
                    showDatePicker = false
                }) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Annulla")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuovo Task") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Torna indietro"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->

        /*
 * scrollState: stato dello scroll della Column.
 * rememberScrollState(): crea e mantiene lo stato attraverso
 * le ricomposizioni — non viene ricreato ad ogni render.
 * Tiene traccia della posizione di scorrimento corrente.
 */
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                /*
                 * padding(16.dp): margine interno ai bordi dello schermo.
                 * Applicato dopo paddingValues — i due padding si sommano.
                 * L'ordine è importante: paddingValues prima, poi il nostro
                 * padding personalizzato — altrimenti il contenuto finisce
                 * sotto la TopAppBar.
                 */
                .padding(16.dp)
                /*
                 * verticalScroll: abilita lo scroll verticale sulla Column.
                 * Senza questo, la Column ha altezza fissa — i campi
                 * che finiscono sotto la tastiera sono irraggiungibili.
                 * Con questo, l'utente può scorrere verso il basso anche
                 * con la tastiera aperta.
                 */
                .verticalScroll(scrollState)
                /*
                 * imePadding(): aggiunge padding dinamico nella parte
                 * inferiore della Column quando la tastiera virtuale (IME —
                 * Input Method Editor) appare.
                 *
                 * PERCHÉ È NECESSARIO INSIEME A adjustResize:
                 * adjustResize ridimensiona la finestra — Compose sa che
                 * lo spazio disponibile è diminuito.
                 * imePadding() usa questa informazione per aggiungere
                 * esattamente il padding necessario a far sì che il
                 * contenuto più in basso (il bottone Salva) rimanga
                 * visibile e raggiungibile sopra la tastiera.
                 *
                 * Uno senza l'altro non funziona completamente:
                 * adjustResize senza imePadding → lo spazio si riduce
                 *   ma il bottone può ancora finire tagliato.
                 * imePadding senza adjustResize → il padding viene
                 *   calcolato ma la finestra non si ridimensiona,
                 *   quindi lo scroll non si attiva correttamente.
                 */
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            /* ── TITOLO ─────────────────────────────────────────────── */
            OutlinedTextField(
                value         = title,
                onValueChange = {
                    title      = it
                    titleError = false
                },
                label          = { Text("Titolo *") },
                isError        = titleError,
                supportingText = {
                    if (titleError) Text("Il titolo è obbligatorio")
                },
                modifier   = Modifier.fillMaxWidth(),
                singleLine = true
            )

            /* ── DESCRIZIONE ────────────────────────────────────────── */
            OutlinedTextField(
                value         = description,
                onValueChange = { description = it },
                label         = { Text("Descrizione") },
                modifier      = Modifier.fillMaxWidth(),
                minLines      = 3
            )

            /* ── PRIORITÀ ───────────────────────────────────────────── */
            Text(
                text  = "Priorità",
                style = MaterialTheme.typography.labelLarge
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Priority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick  = { priority = p },
                        label    = { Text(p.name) }
                    )
                }
            }

            /* ── DATA DI SCADENZA ───────────────────────────────────── */
            Text(
                text  = "Scadenza",
                style = MaterialTheme.typography.labelLarge
            )

            /*
             * FIX DATE PICKER: OutlinedTextField in sola lettura
             * che apre il DatePickerDialog al tocco.
             *
             * readOnly = true: impedisce l'apertura della tastiera —
             * l'utente non può scrivere a mano, può solo toccare.
             *
             * trailingIcon: icona calendario a destra del campo —
             * segnala visivamente che il campo apre un selettore.
             *
             * Il valore mostrato è la data formattata se selezionata,
             * altrimenti un placeholder vuoto.
             */
            OutlinedTextField(
                value         = selectedDate?.format(
                    DateTimeFormatter.ofPattern("dd/MM/yyyy")
                ) ?: "",
                onValueChange = { /* sola lettura — non fa niente */ },
                label         = { Text("Data scadenza") },
                placeholder   = { Text("Tocca per scegliere") },
                readOnly      = true,
                trailingIcon  = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            /*
                             * Icons.Default.DateRange: icona calendario
                             * inclusa nel pacchetto Material Icons Extended.
                             * Assicurarsi che nel build.gradle sia presente:
                             * implementation "androidx.compose.material:material-icons-extended"
                             */
                            imageVector        = Icons.Default.DateRange,
                            contentDescription = "Apri calendario"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            /* ── BOTTONE SALVA ──────────────────────────────────────── */

            /*
             * Spacer con altezza fissa invece di weight(1f):
             * con verticalScroll la Column ha altezza infinita,
             * weight(1f) non funziona. Un margine fisso separa
             * visivamente i campi dal bottone.
             */
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick  = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    onSave(title.trim(), description.trim(), priority, selectedDate)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salva")
            }

            /*
             * Spacer finale: evita che il bottone Salva sia
             * troppo vicino al bordo inferiore dello schermo
             * quando la tastiera è chiusa.
             */
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}