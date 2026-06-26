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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.clatos.dialer.feature.auth.SessionState
import com.clatos.dialer.feature.auth.SessionViewModel
import com.clatos.dialer.navigation.AppNavHost
import com.clatos.dialer.navigation.Routes
import com.clatos.dialer.navigation.topLevelRouteFor
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
                    if (sessionState == SessionState.Loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        // Top-level gating is state-driven: whenever the session
                        // state changes (login, onboarding done, logout/401), jump
                        // to the matching top-level destination and clear the stack.
                        LaunchedEffect(sessionState) {
                            val target = topLevelRouteFor(sessionState)
                            if (navController.currentDestination?.route != target) {
                                navController.navigate(target) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                        AppNavHost(
                            navController = navController,
                            startDestination = topLevelRouteFor(sessionState).ifEmpty { Routes.LOGIN },
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
