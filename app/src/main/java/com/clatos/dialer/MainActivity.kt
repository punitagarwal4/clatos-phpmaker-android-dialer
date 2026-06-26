package com.clatos.dialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.clatos.dialer.feature.auth.SessionViewModel
import com.clatos.dialer.navigation.AppNavHost
import com.clatos.dialer.navigation.startDestinationFor
import com.clatos.dialer.ui.theme.ClatosTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ClatosTheme {
                val sessionViewModel: SessionViewModel = hiltViewModel()
                val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    AppNavHost(
                        navController = navController,
                        startDestination = startDestinationFor(sessionState),
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}
