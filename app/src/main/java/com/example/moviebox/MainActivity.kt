package com.example.moviebox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.moviebox.ui.navigation.AppNavigation
import com.example.moviebox.ui.theme.MovieBoxTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity principal de MovieBox
 * Configurada con Hilt para inyección de dependencias
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieBoxTheme {
                AppNavigation()
            }
        }
    }
}
