package com.clatos.dialer.core.common

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager

/**
 * Helpers for the default phone/dialer app role, which is required for the app's
 * InCallService to drive calls (and is a prerequisite for recording). Handles
 * both the API 29+ RoleManager path and the API 26-28 TelecomManager path.
 */
object DialerRole {

    fun isDefault(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleManager?.isRoleAvailable(RoleManager.ROLE_DIALER) == true &&
            roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
    } else {
        val telecom = context.getSystemService(TelecomManager::class.java)
        telecom?.defaultDialerPackage == context.packageName
    }

    /** Intent that asks the user to make this app the default dialer, or null if unavailable. */
    fun requestIntent(context: Context): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
        if (!roleManager.isRoleAvailable(RoleManager.ROLE_DIALER)) return null
        roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
    } else {
        @Suppress("DEPRECATION")
        Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
            .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
    }
}
