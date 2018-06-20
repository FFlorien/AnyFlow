package be.florien.ampacheplayer.player

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import be.florien.ampacheplayer.AmpacheApp
import be.florien.ampacheplayer.R
import javax.inject.Inject


/**
 * Service used to handle the media player.
 */
class PlayerService : Service() {

    @Inject
    internal lateinit var playerController: PlayerController

    /**
     * Constructor
     */

    override fun onCreate() {
        super.onCreate()
        (application as AmpacheApp).userComponent?.inject(this)
    }

    /**
     * Binder methods
     */

    private val iBinder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        val notification = NotificationCompat.Builder(this, "AmpacheMusic")
                .setContentTitle("Ampache")
                .setContentText("Ampache")
                .setSmallIcon(R.drawable.cover_placeholder)
                .build()
        startForeground(1, notification)
        return iBinder
    }

    inner class LocalBinder : Binder() {
        val service: PlayerController
            get() = playerController
    }
}