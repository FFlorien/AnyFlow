package be.florien.anyflow.player

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.R
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
    private val pendingIntent: PendingIntent by lazy {
        val intent = packageManager?.getLaunchIntentForPackage(packageName)
        PendingIntent.getActivity(this@PlayerService, 0, intent, 0)
    }
    private lateinit var mediaSession: MediaSessionCompat

    private var lastSong: Song? = null

    private val playBackStateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
    private val skipToPreviousAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                this.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
    }
    private val playAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                this.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
    }
    private val pauseAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                this.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE))
    }
    private val skipToNextAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                this.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
    }

    private var subscription: Disposable? = null

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
                        stopForeground(false)
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

    override fun onBind(intent: Intent): IBinder? = iBinder

    override fun onDestroy() {
        super.onDestroy()
        subscription?.dispose()
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

    private fun setPlaybackState(playbackState: Int, position: Long) {
        playBackStateBuilder.setState(playbackState, position, 1.0f)
        mediaSession.setPlaybackState(playBackStateBuilder.build())
    }

    private fun updateNotification(song: Song, albumArt: Bitmap? = null) {
        val notificationBuilder = NotificationCompat.Builder(this, MEDIA_SESSION_NAME)
                .setContentTitle(getString(R.string.notification_title_artist, song.title, song.artistName))
                .setContentText(song.albumName)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.notif)
                .setColor(ContextCompat.getColor(this, R.color.primary))

        var playPauseIndex = 0
        if (playingQueue.listPosition > 0) {
            notificationBuilder.addAction(skipToPreviousAction)
            ++playPauseIndex
        }
        notificationBuilder.addAction(if (playerController.isPlaying()) pauseAction else playAction)
        if (playingQueue.listPosition < playingQueue.itemsCount - 1) {
            notificationBuilder.addAction(skipToNextAction)
        }
        notificationBuilder.setStyle(MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(playPauseIndex))

        val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.albumArtistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.time.toLong())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, song.genre)

        if (albumArt != null) {
            notificationBuilder.setLargeIcon(albumArt)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
        }
        mediaSession.setMetadata(metadataBuilder.build())
        val notification = notificationBuilder.build()
        if (playerController.isPlaying()) {
            startForeground(1, notification)
        } else {
            NotificationManagerCompat.from(this).notify(1, notification)
        }
    }

    companion object {
        private const val MEDIA_SESSION_NAME = "AnyFlow"
    }
}