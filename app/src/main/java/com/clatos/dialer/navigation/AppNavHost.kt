package com.clatos.dialer.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.clatos.dialer.feature.calllog.CallLogScreen
import com.clatos.dialer.feature.contacts.ContactCreateScreen
import com.clatos.dialer.feature.contacts.ContactProfileScreen
import com.clatos.dialer.feature.contacts.ContactsScreen
import com.clatos.dialer.feature.dialer.DialerScreen
import com.clatos.dialer.feature.settings.SettingsScreen

/**
 * In-app navigation (post-authentication, post-permission-gate). Login and the
 * permission gate are rendered directly by MainActivity based on session state,
 * so they are not part of this graph.
 */
object Routes {
    const val DIALER = "dialer"
    const val CALL_LOG = "calllog"
    const val CONTACTS = "contacts"
    const val CONTACT_CREATE = "contacts/create"
    const val CONTACT_PROFILE = "contacts/{contactId}"
    const val SETTINGS = "settings"
    // Encode so ids containing ':' or '/' (e.g. device lookup keys) stay one segment.
    fun contactProfile(contactId: String) = "contacts/${Uri.encode(contactId)}"
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
        composable(Routes.DIALER) {
            DialerScreen(
                onOpenContacts = { navController.navigate(Routes.CONTACTS) },
                onOpenHistory = { navController.navigate(Routes.CALL_LOG) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.CALL_LOG) { CallLogScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.SETTINGS) { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.CONTACTS) {
            ContactsScreen(
                onCreate = { navController.navigate(Routes.CONTACT_CREATE) },
                onOpen = { id -> navController.navigate(Routes.contactProfile(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.CONTACT_CREATE) {
            ContactCreateScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Routes.CONTACT_PROFILE,
            arguments = listOf(navArgument("contactId") { type = NavType.StringType }),
        ) { ContactProfileScreen(onBack = { navController.popBackStack() }) }
    }
}
