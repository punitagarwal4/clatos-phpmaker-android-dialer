package com.clatos.dialer.feature.onboarding

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.clatos.dialer.core.common.PermissionUtils

/**
 * Guided onboarding (US-2.1, US-3.1): request the default-dialer role and all
 * runtime permissions, each with rationale, and gate completion on the critical
 * permissions. Statuses refresh whenever the screen resumes (e.g. returning
 * from system settings) so re-prompting works.
 */
@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current

    // Bumping this recomputes the granted/role checks below.
    var refreshTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refreshTick++
        onPauseOrDispose { }
    }

    val permissions = remember { PermissionUtils.required() }
    // Keyed on refreshTick so statuses recompute after permission/role changes.
    val statuses = remember(refreshTick) {
        permissions.map { it to PermissionUtils.isGranted(context, it.permission) }
    }
    val criticalGranted = remember(refreshTick) { PermissionUtils.criticalGranted(context) }
    val isDefaultDialer = remember(refreshTick) { isDefaultDialer(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { refreshTick++ }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshTick++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Set up Clatos Dialer", fontWeight = FontWeight.Bold)
        Text(
            "Grant the permissions below and make Clatos your default phone app " +
                "so calls can be placed, recorded, and logged to the CRM.",
            modifier = Modifier.padding(vertical = 8.dp),
        )

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.padding(end = 8.dp)) {
                        Text("Default phone app", fontWeight = FontWeight.Bold)
                        Text("Required to record and manage calls.")
                    }
                    Text(if (isDefaultDialer) "✓" else "—")
                }
                OutlinedButton(
                    onClick = { requestDefaultDialer(context, roleLauncher::launch) },
                    modifier = Modifier.padding(top = 8.dp),
                ) { Text(if (isDefaultDialer) "Set again" else "Set as default") }
            }
        }

        statuses.forEach { (info, granted) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.padding(end = 8.dp)) {
                    Text(info.label + if (info.critical) " (required)" else "", fontWeight = FontWeight.Bold)
                    Text(info.rationale)
                }
                Text(if (granted) "✓" else "—")
            }
        }

        Button(
            onClick = { permissionLauncher.launch(PermissionUtils.allPermissionStrings()) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text("Grant permissions") }

        if (!criticalGranted) {
            Text(
                "Grant the required permissions to finish setup.",
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (!isDefaultDialer) {
            Text(
                "Recording needs Clatos to be your default phone app.",
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Button(
            onClick = { viewModel.completeOnboarding() },
            enabled = criticalGranted,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) { Text("Finish setup") }
    }
}

private fun isDefaultDialer(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
    return roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
        roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
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
