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
import com.clatos.dialer.feature.settings.SettingsScreen

/** Route constants for the app's navigation graph. */
object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val DIALER = "dialer"
    const val CALL_LOG = "calllog"
    const val CONTACTS = "contacts"
    const val CONTACT_CREATE = "contacts/create"
    const val CONTACT_PROFILE = "contacts/{contactId}"
    const val SETTINGS = "settings"
    fun contactProfile(contactId: String) = "contacts/$contactId"
}

/** Maps the session state to the top-level destination that gates the app. */
fun topLevelRouteFor(state: SessionState): String = when (state) {
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
        // Top-level destinations are driven by session state in MainActivity;
        // login success and onboarding completion flip that state, which
        // triggers navigation — so these screens don't navigate themselves.
        composable(Routes.LOGIN) { LoginScreen() }
        composable(Routes.ONBOARDING) { OnboardingScreen() }
        composable(Routes.DIALER) {
            DialerScreen(
                onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                onOpenHistory = { navController.navigate(Routes.CALL_LOG) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CALL_LOG) { CallLogScreen() }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
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
