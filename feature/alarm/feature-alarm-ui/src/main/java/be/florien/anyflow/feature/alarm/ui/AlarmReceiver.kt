package be.florien.anyflow.feature.alarm.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import be.florien.anyflow.feature.player.service.ALARM_ACTION
import be.florien.anyflow.feature.player.service.PlayerService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, PlayerService::class.java)//todo inject playerService intent
        serviceIntent.action = ALARM_ACTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}