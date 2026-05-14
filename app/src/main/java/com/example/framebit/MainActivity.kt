package com.example.framebit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.framebit.ui.navigation.AppNavigation
import com.example.framebit.ui.theme.MovieBoxTheme
import dagger.hilt.android.AndroidEntryPoint
import com.example.framebit.auth.AuthManager
import javax.inject.Inject

/**
 * Activity principal de MovieBox
 * Configurada con Hilt para inyección de dependencias
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MovieBoxTheme {
                AppNavigation(authManager = authManager)
            }
        }
    }
}
