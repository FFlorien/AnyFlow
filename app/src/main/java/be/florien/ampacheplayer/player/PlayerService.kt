package be.florien.ampacheplayer.player

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import be.florien.ampacheplayer.AmpacheApp
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
        return iBinder
    }

    inner class LocalBinder : Binder() {
        val service: PlayerController
            get() = playerController
    }
}