package com.clatos.dialer.feature.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.clatos.dialer.core.common.DialerRole
import com.clatos.dialer.core.common.PermissionUtils

/**
 * Hard permission gate for the non-MDM case. The app is unusable until EVERY
 * required runtime permission and the default-dialer role are granted. The user
 * is walked through each one with a rationale, an Allow button, and — for
 * permissions they permanently denied — instructions to enable them in Settings.
 * Re-checks on every resume, so granting/revoking outside the app is reflected.
 */
@Composable
fun PermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }

    val ready = remember(tick) {
        PermissionUtils.allGranted(context) && DialerRole.isDefault(context)
    }

    if (ready) {
        val viewModel: PermissionGateViewModel = hiltViewModel()
        LaunchedEffect(Unit) { viewModel.onPermissionsReady() }
        content()
    } else {
        GuidedPermissionScreen(onChanged = { tick++ })
    }
}

@Composable
private fun GuidedPermissionScreen(onChanged: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    var tick by remember { mutableIntStateOf(0) }
    var requestedOnce by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        tick++
        onChanged()
        onPauseOrDispose { }
    }

    val permissions = remember { PermissionUtils.required() }
    val statuses = remember(tick) {
        permissions.map { it to PermissionUtils.isGranted(context, it.permission) }
    }
    val isDialer = remember(tick) { DialerRole.isDefault(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { requestedOnce = true; tick++; onChanged() }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { tick++; onChanged() }

    val grantedCount = statuses.count { it.second }
    val allPermsGranted = grantedCount == statuses.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Set up Clatos Dialer", fontWeight = FontWeight.Bold)
        Text(
            "Clatos needs the permissions below to place calls and record them. " +
                "The app stays locked until every permission is granted and Clatos is your default phone app.",
            modifier = Modifier.padding(vertical = 8.dp),
        )

        // Default phone app step.
        StepCard(
            title = "Default phone app",
            detail = "Required to place, receive, and record calls.",
            granted = isDialer,
        ) {
            if (!isDialer) {
                OutlinedButton(onClick = {
                    DialerRole.requestIntent(context)?.let { roleLauncher.launch(it) }
                }) { Text("Set as default") }
            }
        }

        // One step per permission.
        statuses.forEach { (info, granted) ->
            val permanentlyDenied = !granted && requestedOnce && activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, info.permission)

            StepCard(
                title = info.label,
                detail = info.rationale,
                granted = granted,
            ) {
                if (!granted && permanentlyDenied) {
                    Text(
                        "Permission was denied. Open Settings → Permissions and enable “${info.label}”.",
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedButton(onClick = { context.startActivity(appSettingsIntent(context)) }) {
                        Text("Open settings")
                    }
                }
            }
        }

        val missing = statuses.filter { !it.second }.map { it.first.permission }
        if (missing.isNotEmpty()) {
            Button(
                onClick = { permissionLauncher.launch(missing.toTypedArray()) },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) { Text("Allow permissions") }
        }

        Text(
            text = when {
                allPermsGranted && isDialer -> "All set — opening Clatos…"
                else -> "$grantedCount of ${statuses.size} permissions granted" +
                    if (isDialer) "" else " · default phone app not set"
            },
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun StepCard(
    title: String,
    detail: String,
    granted: Boolean,
    action: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.padding(end = 8.dp)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(detail)
                }
                Text(if (granted) "✓" else "•")
            }
            action()
        }
    }
}

private fun appSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
