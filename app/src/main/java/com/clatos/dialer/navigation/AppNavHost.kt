package com.clatos.dialer.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.clatos.dialer.feature.auth.LoginScreen
import com.clatos.dialer.feature.auth.SessionState
import com.clatos.dialer.feature.calllog.CallLogScreen
import com.clatos.dialer.feature.contacts.ContactCreateScreen
import com.clatos.dialer.feature.contacts.ContactProfileScreen
import com.clatos.dialer.feature.contacts.ContactsScreen
import com.clatos.dialer.feature.dialer.DialerScreen
import com.clatos.dialer.feature.onboarding.OnboardingScreen

/** Route constants for the app's navigation graph. */
object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val DIALER = "dialer"
    const val CALL_LOG = "calllog"
    const val CONTACTS = "contacts"
    const val CONTACT_CREATE = "contacts/create"
    const val CONTACT_PROFILE = "contacts/{contactId}"
    fun contactProfile(contactId: String) = "contacts/$contactId"
}

/** Picks the first screen based on the current session state. */
fun startDestinationFor(state: SessionState): String = when (state) {
    SessionState.Loading, SessionState.Unauthenticated -> Routes.LOGIN
    SessionState.NeedsOnboarding -> Routes.ONBOARDING
    SessionState.Ready -> Routes.DIALER
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = { navController.navigate(Routes.ONBOARDING) })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onComplete = {
                navController.navigate(Routes.DIALER) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.DIALER) {
            DialerScreen(
                onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                onOpenHistory = { navController.navigate(Routes.CALL_LOG) },
            )
        }
        composable(Routes.CALL_LOG) { CallLogScreen() }
        composable(Routes.CONTACTS) {
            ContactsScreen(
                onCreate = { navController.navigate(Routes.CONTACT_CREATE) },
                onOpen = { id -> navController.navigate(Routes.contactProfile(id)) },
            )
        }
        composable(Routes.CONTACT_CREATE) {
            ContactCreateScreen(onSaved = { navController.popBackStack() })
        }
        composable(Routes.CONTACT_PROFILE) { ContactProfileScreen() }
    }
}
