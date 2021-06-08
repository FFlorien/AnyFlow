package be.florien.anyflow.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LifecycleService
import androidx.media.session.MediaButtonReceiver
import be.florien.anyflow.AnyFlowApp
import javax.inject.Inject


/**
 * Service used to handle the media player.
 */
class PlayerService : LifecycleService() {

    /**
     * Injection
     */

    @Inject
    internal lateinit var playerController: PlayerController

    @Inject
    internal lateinit var playingQueue: PlayingQueue

    /**
     * Fields
     */

    private val iBinder = LocalBinder()
    private val pendingIntent: PendingIntent by lazy {
        val intent = packageManager?.getLaunchIntentForPackage(packageName)
        PendingIntent.getActivity(this@PlayerService, 0, intent, 0)
    }
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationBuilder: PlayerNotificationBuilder
    private val playBackStateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )

    private val isPlaying
        get() = playerController.isPlaying()

    private val hasPrevious
        get() = playingQueue.listPosition > 0

    /**
     * Lifecycle
     */

    override fun onCreate() {
        super.onCreate()
        (application as AnyFlowApp).userComponent?.inject(this)
        mediaSession = MediaSessionCompat(this, MEDIA_SESSION_NAME).apply {
            setSessionActivity(pendingIntent)
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onSeekTo(pos: Long) {
                    playerController.seekTo(pos)
                }

                override fun onSkipToPrevious() {
                    playingQueue.listPosition--
                }

                override fun onPlay() {
                    playerController.resume()
                }

                override fun onSkipToNext() {
                    playingQueue.listPosition++
                }

                override fun onPause() {
                    playerController.pause()
                }
            })
            isActive = true
        }
        setPlaybackState(PlaybackStateCompat.STATE_NONE, 0L)

        notificationBuilder = PlayerNotificationBuilder(this, pendingIntent, mediaSession)

        playerController.stateChangeNotifier.observe(this) {
            if (!isPlaying) {
                stopForeground(false)
            }
            val playbackState = when (it) {
                PlayerController.State.BUFFER -> PlaybackStateCompat.STATE_BUFFERING
                PlayerController.State.RECONNECT -> PlaybackStateCompat.STATE_BUFFERING
                PlayerController.State.PLAY -> PlaybackStateCompat.STATE_PLAYING
                PlayerController.State.PAUSE -> PlaybackStateCompat.STATE_PAUSED
                else -> PlaybackStateCompat.STATE_NONE
            }

            setPlaybackState(playbackState, 0)

        }
        playingQueue.currentSong.observe(this) {
            notificationBuilder.updateNotification(it, isPlaying, hasPrevious, true)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return iBinder
    }

    /**
     * Inner classes
     */

    inner class LocalBinder : Binder() {
        val service: PlayerController
            get() = playerController
    }

    /**
     * Private Methods
     */

    private fun setPlaybackState(playbackState: Int, position: Long) {
        playBackStateBuilder.setState(playbackState, position, 1.0f)
        mediaSession.setPlaybackState(playBackStateBuilder.build())
    }

    companion object {
        const val MEDIA_SESSION_NAME = "AnyFlow"
    }
}