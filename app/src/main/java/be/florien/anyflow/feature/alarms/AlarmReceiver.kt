package be.florien.anyflow.feature.alarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import be.florien.anyflow.feature.player.services.ALARM_ACTION
import be.florien.anyflow.feature.player.services.PlayerService

class AlarmReceiver : BroadcastReceiver() {
    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val serviceIntent = Intent(context, PlayerService::class.java)
        serviceIntent.action = ALARM_ACTION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}