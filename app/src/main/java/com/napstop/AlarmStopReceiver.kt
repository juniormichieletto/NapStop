package com.napstop

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationManager

class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.napstop.ACTION_STOP_ALARM") {
            // Stop the ringing and vibration
            AppRepository.isAlarmActive.value = false
            AlarmController.stopAlarm()
            
            // Dismiss the alert notification (ID 2)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(2)
        }
    }
}
