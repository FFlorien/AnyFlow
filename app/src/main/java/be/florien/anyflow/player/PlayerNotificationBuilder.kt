package be.florien.anyflow.player

import android.app.PendingIntent
import android.app.Service
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import be.florien.anyflow.R
import be.florien.anyflow.extension.GlideApp
import be.florien.anyflow.data.local.model.Song
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class PlayerNotificationBuilder(
        private val service: Service,
        private val pendingIntent: PendingIntent,
        private val mediaSession: MediaSessionCompat) {

    private var state: State = State(null, false, false, false)
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var metadataBuilder: MediaMetadataCompat.Builder

    private val skipToPreviousAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                service.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
    }
    private val playAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                service.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PLAY_PAUSE))
    }
    private val pauseAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                service.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_PLAY_PAUSE))
    }
    private val skipToNextAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                service.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(service, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
    }

    fun updateNotification(song: Song, isPlaying: Boolean, hasPrevious: Boolean, hasNext: Boolean) {
        val newState = State(song, isPlaying, hasPrevious, hasNext)

        if (newState == state) {
            return
        }
        val shouldChangeForegroundState = state.shouldUpdateForeground(newState)
        state = newState
        notificationBuilder = NotificationCompat.Builder(service, PlayerService.MEDIA_SESSION_NAME)
                .setContentTitle(service.getString(R.string.notification_title_artist, song.title, song.artistName))
                .setContentText(song.albumName)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.notif)
                .setColor(ContextCompat.getColor(service, R.color.primary))

        var playPauseIndex = 0
        if (hasPrevious) {
            notificationBuilder.addAction(skipToPreviousAction)
            ++playPauseIndex
        }
        notificationBuilder.addAction(if (isPlaying) pauseAction else playAction)
        if (hasNext) {
            notificationBuilder.addAction(skipToNextAction)
        }
        notificationBuilder.setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(playPauseIndex))

        metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.albumArtistName)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.albumName)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.time.toLong())
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE, song.genre)
        GlideApp
                .with(service)
                .asBitmap()
                .load(song.art)
                .listener(asyncBitmapLoadingReceiver)
                .submit()
        notifyChange(shouldChangeForegroundState)
    }

    private fun notifyChange(updateForegroundState: Boolean) {
        mediaSession.setMetadata(metadataBuilder.build())
        val notification = notificationBuilder.build()
        if (updateForegroundState) {
            if (state.isPlaying) {
                service.startForeground(1, notification)
            } else {
                service.stopForeground(false)
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
            notificationBuilder.setLargeIcon(resource)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, resource)
            notifyChange(false)
            return true
        }

        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
            return true
        }
    }

    private data class State(val song: Song?, val isPlaying: Boolean, val hasPrevious: Boolean, val hasNext: Boolean) {
        fun shouldUpdateForeground(newState: State) = newState.isPlaying != isPlaying
    }
}