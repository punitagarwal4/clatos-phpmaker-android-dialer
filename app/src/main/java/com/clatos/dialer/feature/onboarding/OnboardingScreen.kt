package com.clatos.dialer.feature.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Guided onboarding: request the default-dialer role and all runtime permissions.
 * (US-2.1, US-3.1). Permission rationale UI is simplified for the scaffold.
 */
@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* handle grants; re-prompt for denials in a full build */ }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* user returned from default-dialer prompt */ }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Set up Clatos Dialer", modifier = Modifier.padding(bottom = 16.dp))

        Button(
            onClick = { permissionLauncher.launch(REQUIRED_PERMISSIONS) },
            modifier = Modifier.padding(bottom = 8.dp),
        ) { Text("Grant permissions") }

        Button(
            onClick = { requestDefaultDialer(context, roleLauncher::launch) },
            modifier = Modifier.padding(bottom = 16.dp),
        ) { Text("Set as default phone app") }

        Button(onClick = onComplete) { Text("Continue") }
    }
}

private fun requestDefaultDialer(context: Context, launch: (Intent) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true &&
            !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
        ) {
            launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
        }
    }
}

private val REQUIRED_PERMISSIONS: Array<String> = buildList {
    add(android.Manifest.permission.CALL_PHONE)
    add(android.Manifest.permission.READ_PHONE_STATE)
    add(android.Manifest.permission.READ_CALL_LOG)
    add(android.Manifest.permission.READ_CONTACTS)
    add(android.Manifest.permission.WRITE_CONTACTS)
    add(android.Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(android.Manifest.permission.POST_NOTIFICATIONS)
        add(android.Manifest.permission.READ_MEDIA_AUDIO)
    }
}.toTypedArray()
