package com.academy.taskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.academy.taskflow.ui.theme.TaskFlowTheme
import dagger.hilt.android.AndroidEntryPoint
import com.academy.taskflow.navigation.AppNavigation

/*
 * @AndroidEntryPoint: rende questa Activity un punto di iniezione Hilt.
 * OBBLIGATORIO su ogni Activity/Fragment che usa Hilt o hiltViewModel().
 * Senza questa annotation Hilt non può iniettare dipendenze nell'Activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskFlowTheme {
                AppNavigation()
            }
        }
    }
}
