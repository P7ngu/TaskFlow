package com.academy.taskflow.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.academy.taskflow.ui.screens.TaskDetailScreen
import com.academy.taskflow.ui.screens.TaskListScreen
import com.academy.taskflow.viewModel.TaskViewModel


/**Screen è una sealed class con una route di navigazione.
 * Centralizza i nomi in un posto;
 * se dovesse esserci un errore nel nome, allora verrà dato anche
 * un errore di compilazione.
 *
 * Le route sono delle stringhe, la sealed class serve perché
 * senza di essa potremmo anche scrivere task_list in dieci posti diversi
 * e sbagliare una sola volta in uno di questi.*
 *
 *
 * {taskId} è un paramtro del path e verrà sostituito con il valore reale quando si naviga.*/

sealed class Screen(val route: String){
    object TaskList : Screen("task_list")
    object TaskDetail : Screen("task_detail/{taskId}") {
        /**createRoute --> mi costruisce la route con l'id reale*/
        fun createRoute(taskId: String) = "task_detail/$taskId"
    }
}

/**AppNavigation: è il grafo di navigazione completo ed è il
 * file che decide quali schermate esistono, come si raggiungono e come si passa
 * da una schermata all'altra
 *
 * viewModel() --> mi crea o recupera il ViewModel nello stesso scope.
 * Condiviso tra tutte le schermate dentro il NavHost.
 * */
@Composable
fun AppNavigation(viewModel: TaskViewModel = viewModel()){
    val navController = rememberNavController()

    /**collectAsStateWithLifecyle: si fermerà in background.
     * Grazie a ciò risparmierà risorse (esempio: risparmio batteria).
     * Avere l'app in background significa avere l'app non visbile.
     * Foreground = l'app è in primo piano in funzione.
     * delegation by = uiState è già un valore, ma non lo è il wrapper State<T>*/

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    /**Per far sì che NavHost venga letto, si dovrà aggiungere nella libreria delle dependencies
     * di build.gradle.kt (Module: app) questo --> implementation(libs.androidx.navigation.compose) "
     **/
    NavHost (
        navController = navController,
        startDestination = Screen.TaskList.route
    ) {
        /**Schermata list task.
         * onTaskCLick: naviga al dettaglio del task passando l'id nel path;
         * onToggleDone: delega al ViewModel*/
        composable(Screen.TaskList.route) {
            TaskListScreen(
                //Lo segnala in rosso perché manca il file TaskListScreen
                uiState = uiState,
                onTaskClick = {
                    id ->
                    navController.navigate(
                        Screen.TaskDetail.createRoute(id)
                    )
                },
                onToggleDone = {
                    id -> viewModel.toggleDone(id)
                }
            )
        }

        /**Schermata dettaglio task.
         * arguments: dichiara taskId come parametro String all'interno del path
         * backStack: contiene i valori reali dei parametri*/
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                }
            )
        ) {
            backStack ->
            val taskId = backStack.arguments?.getString("taskId") ?: ""

            TaskDetailScreen(
                task = viewModel.getTaskById(taskId),
                /**
                 * - popBackStack: rimuove la destinazione corrente e torna
                 * a quella precedente. Questo utilizzo è corretto
                 * per il tasto "back".
                 *
                 * - con navigate() si potrebbe aggiungere una nuova destinazione
                 *
                 * Per semplicità didattica */
                onBack = { navController.popBackStack() },
                onToggleDone = { viewModel.toggleDone(taskId) }
            )
        }
    }
}
