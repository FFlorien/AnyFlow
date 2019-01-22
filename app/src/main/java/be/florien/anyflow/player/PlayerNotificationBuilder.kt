package be.florien.anyflow.player

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import be.florien.anyflow.R
import be.florien.anyflow.persistence.local.model.Song

class PlayerNotificationBuilder(context: Context,
                                private val mediaSession: MediaSessionCompat,
                                private val startingIntent: PendingIntent) {

    private val skipToPreviousAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                context.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS))
    }
    private val playAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                context.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE))
    }
    private val pauseAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE))
    }
    private val skipToNextAction by lazy {
        NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                context.getString(R.string.app_name),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT))
    }

    fun getUpdatedNotification(context: Context, song: Song, isPlaying: Boolean, isFirstInPlaylist: Boolean, isLastInPlaylist: Boolean, albumArt: Bitmap? = null): Notification {
        val notificationBuilder = NotificationCompat.Builder(context, "AnyFlow")
                .setContentTitle("${song.title} by ${song.artistName}")
                .setContentText(song.albumName)
                .setContentIntent(startingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.notif)
                .setColor(ContextCompat.getColor(context, R.color.primary))

        var playPauseIndex = 0
        if (!isFirstInPlaylist) {
            notificationBuilder.addAction(skipToPreviousAction)
            ++playPauseIndex
        }
        notificationBuilder.addAction(if (isPlaying) pauseAction else playAction)
        if (isLastInPlaylist) {
            notificationBuilder.addAction(skipToNextAction)
        }
        notificationBuilder.setStyle(android.support.v4.media.app.NotificationCompat.MediaStyle()
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
        return notificationBuilder.build()
    }

}