package com.clatos.dialer.core.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService

/**
 * Single place to start an outgoing call so the dialpad, contacts, and call
 * history all behave the same (US-2.5). Requires CALL_PHONE; callers should
 * gate the UI on that permission, but this also no-ops safely without it.
 */
object CallPlacer {

    /** @return true if the call was dispatched. */
    fun placeCall(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val telecom = context.getSystemService<TelecomManager>() ?: return false
        return runCatching {
            telecom.placeCall(Uri.fromParts("tel", number, null), null)
        }.isSuccess
    }
}
