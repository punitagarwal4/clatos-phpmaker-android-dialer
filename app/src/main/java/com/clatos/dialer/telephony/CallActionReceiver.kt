package com.clatos.dialer.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles the Answer/Decline actions on the incoming-call notification so the
 * agent can act without opening the in-call screen. Routes to [CallManager],
 * which owns the active telecom Call.
 */
@AndroidEntryPoint
class CallActionReceiver : BroadcastReceiver() {

    @Inject lateinit var callManager: CallManager

    override fun onReceive(context: Context, intent: Intent) {
        // Triggers Hilt field injection for the receiver.
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_ANSWER -> {
                callManager.answer()
                CallNotifications.launchInCall(context)
            }
            ACTION_DECLINE -> callManager.reject()
        }
        CallNotifications.cancel(context)
    }

    companion object {
        const val ACTION_ANSWER = "com.clatos.dialer.action.ANSWER"
        const val ACTION_DECLINE = "com.clatos.dialer.action.DECLINE"
    }
}
