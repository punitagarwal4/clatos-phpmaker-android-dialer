package com.clatos.dialer.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Central definition of the runtime permissions the app needs, plus helpers to
 * check grant status. Used by onboarding (request) and feature screens
 * (resilience: detect revoked permissions). See EPIC 3.
 */
object PermissionUtils {

    data class PermissionInfo(
        val permission: String,
        val label: String,
        val rationale: String,
        val critical: Boolean,
    )

    /** All runtime permissions, version-aware, in request order. */
    fun required(): List<PermissionInfo> = buildList {
        add(PermissionInfo(Manifest.permission.CALL_PHONE, "Phone calls", "Place and manage calls from the dialer.", critical = true))
        add(PermissionInfo(Manifest.permission.READ_PHONE_STATE, "Phone state", "Detect call start/end to record and log calls.", critical = true))
        add(PermissionInfo(Manifest.permission.RECORD_AUDIO, "Microphone", "Record calls for upload to the CRM.", critical = true))
        add(PermissionInfo(Manifest.permission.READ_CALL_LOG, "Call log", "Capture missed/incoming/outgoing call history.", critical = false))
        add(PermissionInfo(Manifest.permission.READ_CONTACTS, "Read contacts", "Show device contacts in the unified list.", critical = false))
        add(PermissionInfo(Manifest.permission.WRITE_CONTACTS, "Write contacts", "Save contacts you create to the device (optional).", critical = false))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(PermissionInfo(Manifest.permission.POST_NOTIFICATIONS, "Notifications", "Show the active-call recording notification.", critical = false))
            add(PermissionInfo(Manifest.permission.READ_MEDIA_AUDIO, "Audio files", "Manage recorded audio before upload.", critical = false))
        }
    }

    fun allPermissionStrings(): Array<String> = required().map { it.permission }.toTypedArray()

    fun isGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    /** True when EVERY required runtime permission is granted (hard app gate). */
    fun allGranted(context: Context): Boolean =
        required().all { isGranted(context, it.permission) }

    /** Required permissions that are still not granted. */
    fun missing(context: Context): List<PermissionInfo> =
        required().filter { !isGranted(context, it.permission) }

    /** True when every critical permission is granted. */
    fun criticalGranted(context: Context): Boolean =
        required().filter { it.critical }.all { isGranted(context, it.permission) }
}
