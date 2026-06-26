package com.clatos.dialer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.clatos.dialer.feature.auth.LoginScreen
import com.clatos.dialer.feature.auth.SessionState
import com.clatos.dialer.feature.auth.SessionViewModel
import com.clatos.dialer.feature.onboarding.PermissionGate
import com.clatos.dialer.navigation.AppNavHost
import com.clatos.dialer.navigation.Routes
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

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    val contentModifier = Modifier.padding(padding)
                    when (sessionState) {
                        SessionState.Loading ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }

                        SessionState.Unauthenticated ->
                            LoginScreen(modifier = contentModifier)

                        // Authenticated: the permission gate keeps the app locked
                        // until all runtime permissions + the default-dialer role
                        // are granted, then renders the in-app navigation.
                        SessionState.Authenticated -> PermissionGate {
                            val navController = rememberNavController()
                            AppNavHost(
                                navController = navController,
                                startDestination = Routes.DIALER,
                                modifier = contentModifier,
                            )
                        }
                    }
                }
            }
        }
    }
}
