package com.clatos.dialer.telephony

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.clatos.dialer.R
import com.clatos.dialer.feature.incall.InCallActivity

/**
 * Notifications/launch for the in-call UI. Incoming calls use a high-priority
 * full-screen-intent notification (the correct way to surface a ringing call
 * over the lock screen from a background service); outgoing/active calls launch
 * the in-call activity directly since they're user-initiated.
 */
object CallNotifications {
    private const val CHANNEL_ID = "ongoing_call"
    private const val NOTIFICATION_ID = 4301

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.call_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }

    fun showIncoming(context: Context, callerLabel: String) {
        ensureChannel(context)
        val fullScreen = PendingIntent.getActivity(
            context, 0, inCallIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.incoming_call_title))
            .setContentText(callerLabel)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .addAction(
                android.R.drawable.sym_action_call,
                context.getString(R.string.call_answer),
                actionIntent(context, CallActionReceiver.ACTION_ANSWER, requestCode = 1),
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.call_decline),
                actionIntent(context, CallActionReceiver.ACTION_DECLINE, requestCode = 2),
            )
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification)
    }

    private fun actionIntent(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, CallActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun launchInCall(context: Context) {
        context.startActivity(inCallIntent(context))
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun inCallIntent(context: Context): Intent =
        Intent(context, InCallActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
}
