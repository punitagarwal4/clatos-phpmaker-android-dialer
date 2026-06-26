package com.clatos.dialer.telephony

import android.telecom.Call
import android.telecom.CallScreeningService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Observes incoming calls for early caller-ID resolution against the unified
 * contacts (device + CRM). We do NOT block calls here; we allow them through
 * and let the in-call UI display the resolved name.
 */
@AndroidEntryPoint
class ClatosCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // Allow the call; no blocking/silencing for an agent dialer.
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, response)
        // TODO: resolve callDetails.handle against ContactRepository for caller ID.
    }
}
