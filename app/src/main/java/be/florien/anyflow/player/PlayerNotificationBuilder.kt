package be.florien.anyflow.player

import android.app.PendingIntent
import android.app.Service
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.session.MediaButtonReceiver
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.extension.stopForegroundAndKeepNotification
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class PlayerNotificationBuilder(
        private val service: Service,
        private val mediaSession: MediaSessionCompat,
        pendingIntent: PendingIntent,
private val dataRepository: DataRepository) {

    private var state: State = State(isPlaying = false, hasPrevious = false, hasNext = false)
    var lastPosition = 0L
    var lastPlaybackState = PlaybackStateCompat.STATE_NONE
        set(value) {
            field = value
            setPlaybackState()
        }
    private val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(service, PlayerService.MEDIA_SESSION_NAME)
            .setSmallIcon(R.drawable.notif)
            .setContentIntent(pendingIntent)
            .setVibrate(null)
    private val playBackStateBuilder: PlaybackStateCompat.Builder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
    private lateinit var metadataBuilder: MediaMetadataCompat.Builder

    private val skipToPreviousAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                "Previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
    }
    private val playAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PLAY))
    }
    private val pauseAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "Pause",
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PAUSE))
    }
    private val skipToNextAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
    }

    fun updateNotification(isPlaying: Boolean, hasPrevious: Boolean, hasNext: Boolean) {
        val newState = State(isPlaying, hasPrevious, hasNext)

        if (newState == state) {
            return
        }
        val shouldChangeForegroundState = state.shouldUpdateForeground(newState)
        state = newState
        notificationBuilder.clearActions()

        var actionsIndexes = intArrayOf(0)
        //if (hasPrevious) {
            notificationBuilder.addAction(skipToPreviousAction)
            actionsIndexes = intArrayOf(*actionsIndexes, 1)
        //}
        notificationBuilder.addAction(if (isPlaying) pauseAction else playAction)
        if (hasNext) {
            notificationBuilder.addAction(skipToNextAction)
            actionsIndexes = intArrayOf(*actionsIndexes, 2)
        }
        notificationBuilder.setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(*actionsIndexes))

        notifyChange(shouldChangeForegroundState)
    }

    fun updateMediaSession(song: SongInfo) {
        metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.albumArtistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, song.genreNames.first())
        GlideApp
                .with(service)
                .asBitmap()
                .load(dataRepository.getAlbumArtUrl(song.albumId))
                .listener(asyncBitmapLoadingReceiver)
                .submit()
        mediaSession.setMetadata(metadataBuilder.build())
        notifyChange(false)
    }

    private fun setPlaybackState() {
        playBackStateBuilder.setState(lastPlaybackState, 0L, 1.0f)
        mediaSession.setPlaybackState(playBackStateBuilder.build())
        notifyChange(false)
    }

    private fun notifyChange(updateForegroundState: Boolean) {
        val notification = notificationBuilder.build()
        if (updateForegroundState) {
            if (state.isPlaying) {
                service.startForeground(1, notification)
            } else {
                service.stopForegroundAndKeepNotification()
                NotificationManagerCompat.from(service).notify(1, notification)
            }
        } else {
            NotificationManagerCompat.from(service).notify(1, notification)
        }
    }

    /**
     * Inner classes
     */

    private val asyncBitmapLoadingReceiver = object : RequestListener<Bitmap> {
        override fun onResourceReady(resource: Bitmap?, model: Any?, target: Target<Bitmap>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
            mediaSession.setMetadata(metadataBuilder.build())
            return true
        }

        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
            return true
        }
    }

    private data class State(val isPlaying: Boolean, val hasPrevious: Boolean, val hasNext: Boolean) {
        fun shouldUpdateForeground(newState: State) = newState.isPlaying != isPlaying
    }
}