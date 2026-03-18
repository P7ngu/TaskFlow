package com.academy.taskflow.ui.permissions

// Importiamo le librerie necessarie. Nota l'uso di ContextCompat e ActivityCompat
// che ci garantiscono la retrocompatibilità con le vecchie versioni di Android.
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.saveable.rememberSaveable

/*
 * Manteniamo la tua ottima data class.
 * Separare i dati (il "cosa") dall'interfaccia (il "come") è un'ottima pratica.
 */
private data class PermissionRequest(
    val permission: String,
    val label: String,
    val rationale: String
)

@Composable
fun PermissionHandler() {
    // Otteniamo il Context corrente, fondamentale in Android per accedere alle risorse di sistema.
    val context = LocalContext.current

    // Effettuiamo un cast del Context ad Activity. Ci servirà dopo per usare le API dei permessi.
    val activity = context as? Activity

    // Costruiamo la lista dei permessi in base alla versione di Android (il tuo codice era già perfetto qui).
    val permissionsToRequest = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(PermissionRequest(Manifest.permission.POST_NOTIFICATIONS, "Notifiche", "Per avvisarti delle scadenze dei task."))
            }
            add(PermissionRequest(Manifest.permission.READ_CALENDAR, "Lettura calendario", "Per leggere gli eventi ed evitare sovrapposizioni."))
            add(PermissionRequest(Manifest.permission.WRITE_CALENDAR, "Scrittura calendario", "Per aggiungere i task al tuo calendario."))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(PermissionRequest(Manifest.permission.SCHEDULE_EXACT_ALARM, "Allarmi esatti", "Per i reminder precisi all'ora di scadenza."))
            }
        }
    }

    /* * 1. LO STATO DELL'INDICE (Quale permesso stiamo guardando?)
 * * var         -> Usiamo 'var' (variabile mutabile) perché il valore cambierà (faremo currentIndex++).
 * by          -> Delegazione di Kotlin. Ci permette di leggere e scrivere 'currentIndex' come se
 * fosse un normale numero intero (es. currentIndex = 1), nascondendo la complessità
 * dell'oggetto State che c'è sotto.
 * rememberSaveable -> Il nostro "salvavita" per la rotazione. A differenza del semplice 'remember',
 * prende il valore attuale e lo impacchetta nel "Bundle" di sistema un millisecondo prima
 * che l'Activity venga distrutta dalla rotazione dello schermo. Quando l'Activity rinasce,
 * lo disimballa e lo ripristina. L'app non perde la memoria.
 * mutableIntStateOf(0) -> Crea un contenitore reattivo che parte dal valore 0. Se questo valore cambia,
 * Compose sa che deve "ricomporre" (ridisegnare) la UI.
 * Nota tecnica: usiamo 'mutableIntStateOf' invece di 'mutableStateOf' perché è una
 * versione ottimizzata appositamente per i numeri primitivi, risparmiando memoria (evita il boxing).
 */
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }

    /* * 2. LO STATO DELLA UI (Il dialog è aperto o chiuso?)
     * * var showRationaleDialog -> Il flag booleano che usiamo nell' 'if (showRationaleDialog)' per
     * decidere se disegnare o meno il nostro AlertDialog a due bottoni.
     * rememberSaveable -> Anche qui è fondamentale! Se l'utente sta leggendo il tuo testo
     * "Serve per aggiungere i task al calendario" e ruota il tablet, senza rememberSaveable
     * il dialog svanirebbe improvvisamente (perché lo stato tornerebbe a false).
     * Con rememberSaveable, il dialog rimane stoicamente aperto a schermo.
     * mutableStateOf(false) -> Partiamo dal presupposto che all'inizio il dialog sia invisibile (false).
     */
    var showRationaleDialog by rememberSaveable { mutableStateOf(false) }

    // Registriamo il "Launcher". Questo è il ponte tra la nostra app e il sistema operativo Android.
    // Quando chiameremo launcher.launch(), Android mostrerà il SUO popup ufficiale.
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Questa lambda viene eseguita DOPO che l'utente ha risposto al popup di sistema di Android.
        // Che l'utente abbia accettato (true) o rifiutato (false), noi passiamo al permesso successivo.
        currentIndex++
    }

    // Se abbiamo analizzato tutti i permessi nella lista, usciamo dalla funzione Composable.
    if (currentIndex >= permissionsToRequest.size) return

    // Estraiamo il permesso corrente che dobbiamo analizzare.
    val currentRequest = permissionsToRequest[currentIndex]

    // Effettuiamo il controllo in tempo reale: abbiamo GIÀ questo permesso?
    // Usiamo ContextCompat per evitare crash su versioni Android più vecchie.
    val isAlreadyGranted = ContextCompat.checkSelfPermission(
        context,
        currentRequest.permission
    ) == PackageManager.PERMISSION_GRANTED

    // Utilizziamo un LaunchedEffect per eseguire logica "non-UI" (side-effects) in Compose.
    // Viene rieseguito ogni volta che cambia 'currentIndex'.
    LaunchedEffect(currentIndex) {
        if (isAlreadyGranted) {
            // Caso 1: Abbiamo già il permesso. Ottimo! Saltiamo direttamente al prossimo.
            currentIndex++
        } else {
            // Caso 2: NON abbiamo il permesso. Chiediamo ad Android se l'utente lo ha rifiutato in passato
            // e se dobbiamo quindi mostrargli una spiegazione prima di richiederlo di nuovo.
            val shouldShowRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, currentRequest.permission)
            } ?: false

            if (shouldShowRationale) {
                // Caso 2A: Android dice "Sì, l'utente l'ha rifiutato prima. Spiegagli perché ti serve!".
                // Impostiamo lo stato a true per far apparire il nostro AlertDialog.
                showRationaleDialog = true
            } else {
                // Caso 2B: È la prima volta che lo chiediamo (o l'utente ha bloccato per sempre le richieste).
                // Chiamiamo direttamente il popup di sistema ufficiale di Android, senza il nostro dialog.
                launcher.launch(currentRequest.permission)
            }
        }
    }

    // Se lo stato ci dice di mostrare la spiegazione, disegniamo il nostro Dialog educativo.
    if (showRationaleDialog) {
        AlertDialog(
            // Se l'utente clicca fuori, consideriamolo un "Non ora" e chiudiamo il dialog.
            onDismissRequest = {
                showRationaleDialog = false
                currentIndex++
            },
            title = { Text("Serve il tuo permesso") },
            text = {
                // Qui mostriamo la spiegazione specifica (il rationale) per convincere l'utente.
                Text(currentRequest.rationale)
            },
            confirmButton = {
                // IL BOTTONE MAGICO: Se l'utente capisce la spiegazione e clicca "Ok",
                // chiudiamo il nostro dialog e INVOCHIAMO IL SISTEMA (launcher) per la richiesta vera.
                TextButton(onClick = {
                    showRationaleDialog = false
                    launcher.launch(currentRequest.permission)
                }) {
                    Text("Ok, procedi")
                }
            },
            dismissButton = {
                // Se l'utente rifiuta anche la spiegazione, chiudiamo e passiamo oltre senza disturbare il sistema.
                TextButton(onClick = {
                    showRationaleDialog = false
                    currentIndex++
                }) {
                    Text("Non ora")
                }
            }
        )
    }
}