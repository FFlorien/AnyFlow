package be.florien.anyflow.player

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.persistence.local.model.Song
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


/**
 * Service used to handle the media player.
 */
class PlayerService : Service() {

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
    private val startingIntent: PendingIntent by lazy {
        val intent = packageManager?.getLaunchIntentForPackage(packageName)
        PendingIntent.getActivity(this@PlayerService, 0, intent, 0)
    }
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var playerNotificationBuilder: PlayerNotificationBuilder

    private var lastSong: Song? = null

    private val playBackStateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )

    private var subscription: Disposable? = null
    private var isAnyClientBound = false
        set(value) {
            field = value
            stopSelfIfUnused()
        }
    private var canStopSelf = false
        set(value) {
            field = value
            stopSelfIfUnused()
        }

    private val handler = Handler()
    private val stopServiceDelayed = {
        canStopSelf = true
    }


    /**
     * Lifecycle
     */

    override fun onCreate() {
        super.onCreate()
        (application as AnyFlowApp).userComponent?.inject(this)
        startService(Intent(this,PlayerService::class.java))
        playerController.initialize()
        mediaSession = MediaSessionCompat(this, "AnyFlow").apply {
            setSessionActivity(startingIntent)
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
        playerNotificationBuilder = PlayerNotificationBuilder(this, mediaSession, startingIntent)
        setPlaybackState(PlaybackStateCompat.STATE_NONE, 0L)
        val shutdownWaitingIntent = Intent(this, PlayerService::class.java)
        shutdownWaitingIntent.action = SHUTDOWN
        val intentFilter = IntentFilter()
        intentFilter.addAction(SHUTDOWN)

        subscription = Flowable.combineLatest<PlayerController.State, Song?, Pair<PlayerController.State, Song?>>(
                playerController.stateChangeNotifier,
                playingQueue.currentSongUpdater,
                BiFunction { state, song ->
                    Pair(state, song)
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    if (!playerController.isPlaying()) {
                        handler.postDelayed(stopServiceDelayed, 10 * 1000)
                    } else {
                        handler.removeCallbacks(stopServiceDelayed)
                        canStopSelf = false
                    }
                    val playbackState = when (it.first) {
                        PlayerController.State.BUFFER -> PlaybackStateCompat.STATE_BUFFERING
                        PlayerController.State.RECONNECT -> PlaybackStateCompat.STATE_BUFFERING
                        PlayerController.State.PLAY -> PlaybackStateCompat.STATE_PLAYING
                        PlayerController.State.PAUSE -> PlaybackStateCompat.STATE_PAUSED
                        else -> PlaybackStateCompat.STATE_NONE
                    }

                    setPlaybackState(playbackState, 0)
                    it.second?.let { song ->
                        updateNotification(song)
                        GlideApp
                                .with(this)
                                .asBitmap()
                                .load(song.art)
                                .into(AsyncBitmapLoadingReceiver(song))
                    }
                    lastSong = it.second
                }
                .subscribe()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        isAnyClientBound = true
        return iBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isAnyClientBound = false
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.dispose()
        playerController.release()
        mediaSession.release()
        NotificationManagerCompat.from(this).cancelAll()
    }

    /**
     * Inner classes
     */

    inner class AsyncBitmapLoadingReceiver(val song: Song) : SimpleTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            updateNotification(song, resource)
        }
    }

    inner class LocalBinder : Binder() {
        val service: PlayerController
            get() = playerController
    }

    /**
     * Private Methods
     */

    private fun stopSelfIfUnused() {
        if (canStopSelf && !isAnyClientBound) {
            stopSelf()
        }
    }

    private fun setPlaybackState(playbackState: Int, position: Long) {
        playBackStateBuilder.setState(playbackState, position, 1.0f)
        mediaSession.setPlaybackState(playBackStateBuilder.build())
    }

    private fun updateNotification(song: Song, albumArt: Bitmap? = null) {
        val isFirstInPlaylist = playingQueue.listPosition == 0
        val isLastInPlaylist = playingQueue.listPosition < playingQueue.itemsCount - 1
        val notification = playerNotificationBuilder.getUpdatedNotification(this, song, playerController.isPlaying(), isFirstInPlaylist, isLastInPlaylist, albumArt)
        if (playerController.isPlaying()) {
            startForeground(1, notification)
        } else {
            stopForeground(false)
            NotificationManagerCompat.from(this).notify(1, notification)
        }
    }

    companion object {
        private const val SHUTDOWN = "SHUTDOWN"
    }
}