package com.clatos.dialer.telephony

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Handles the Answer/Decline actions on the incoming-call notification so the
 * agent can act without opening the in-call screen. Routes to [CallManager],
 * which owns the active telecom Call.
 *
 * Uses EntryPointAccessors rather than @AndroidEntryPoint field injection so we
 * don't have to call the abstract BroadcastReceiver.onReceive (which the Kotlin
 * compiler rejects before Hilt's bytecode transform runs).
 */
class CallActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CallActionEntryPoint {
        fun callManager(): CallManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        val callManager = EntryPointAccessors
            .fromApplication(context.applicationContext, CallActionEntryPoint::class.java)
            .callManager()

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
