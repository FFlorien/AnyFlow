package be.florien.anyflow.player

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.R
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.persistence.local.model.Song
import be.florien.anyflow.view.player.PlayerActivity
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.android.schedulers.AndroidSchedulers
import javax.inject.Inject


/**
 * Service used to handle the media player.
 */
class PlayerService : Service() {

    @Inject
    internal lateinit var playerController: PlayerController
    @Inject
    internal lateinit var playingQueue: PlayingQueue

    private val iBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        (application as AnyFlowApp).userComponent?.inject(this)
        playingQueue.currentSongUpdater.observeOn(AndroidSchedulers.mainThread()).doOnNext { song ->
            song?.let {
                displayNotification(it)
                GlideApp
                        .with(this)
                        .asBitmap()
                        .load(song.art)
                        .into(Tutu(it))
            }
        }.subscribe()
    }

    override fun onBind(intent: Intent): IBinder? {
        return iBinder
    }

    inner class Tutu(val song: Song) : SimpleTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            displayNotification(song, resource)
        }
    }

    inner class LocalBinder : Binder() {
        val service: PlayerController
            get() = playerController
    }

    private fun displayNotification(song: Song, albumArt: Bitmap? = null) {
        val intent = Intent(this, PlayerActivity::class.java)
        intent.action = Intent.ACTION_MAIN
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        val notificationBuilder = NotificationCompat.Builder(this, "AnyFlow")
                .setContentTitle("${song.title} by ${song.artistName}")
                .setContentText(song.albumName)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.notif)
                .setColor(ContextCompat.getColor(this, R.color.primary))
        if (albumArt != null) {
            notificationBuilder.setLargeIcon(albumArt)
        }
        startForeground(1, notificationBuilder.build())
    }
}