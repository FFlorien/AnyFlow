package be.florien.anyflow.data

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import be.florien.anyflow.R
import be.florien.anyflow.data.server.exception.NoServerException
import be.florien.anyflow.data.server.exception.SessionExpiredException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.extension.iLog
import kotlinx.coroutines.*
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Named

class SyncService
@Inject constructor() : LifecycleService() {
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }


    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        eLog(throwable, "Received an exception in SyncService's scope")
    }
    private val serviceScope = CoroutineScope(Dispatchers.Main + exceptionHandler)

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    @field:Named("Songs")
    lateinit var songsPercentageUpdater: LiveData<Int>

    @Inject
    @field:Named("Genres")
    lateinit var genresPercentageUpdater: LiveData<Int>

    @Inject
    @field:Named("Albums")
    lateinit var albumsPercentageUpdater: LiveData<Int>

    @Inject
    @field:Named("Playlists")
    lateinit var playlistsPercentageUpdater: LiveData<Int>

    @Inject
    @field:Named("Artists")
    lateinit var artistsPercentageUpdater: LiveData<Int>
    private val pendingIntent: PendingIntent by lazy {
        val intent = packageManager?.getLaunchIntentForPackage(packageName)
        PendingIntent.getActivity(this@SyncService, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onCreate() {
        super.onCreate()
        (application as be.florien.anyflow.AnyFlowApp).serverComponent?.inject(this)
        serviceScope.launch {
            try {
                syncRepository.syncAll()
            } catch (throwable: Throwable) {
                when (throwable) {
                    is SessionExpiredException ->
                        this@SyncService.iLog(throwable, "The session token is expired")
                    is WrongIdentificationPairException ->
                        this@SyncService.iLog(throwable, "Couldn't reconnect the user: wrong user/pwd")
                    is SocketTimeoutException, is NoServerException ->
                        this@SyncService.eLog(throwable, "Couldn't connect to the webservice")
                    else ->
                        this@SyncService.eLog(throwable, "Unknown error")
                }
            }
            stopForeground()
        }
        songsPercentageUpdater.observe(this) {
            if (it in 0..100) {
                notifyChange(getString(R.string.update_songs, it))
            } else {
                stopForeground()
            }
        }
        genresPercentageUpdater.observe(this) {
            if (it in 0..100) {
                notifyChange(getString(R.string.update_genres, it))
            } else {
                stopForeground()
            }
        }
        artistsPercentageUpdater.observe(this) {
            if (it in 0..100) {
                notifyChange(getString(R.string.update_artists, it))
            } else {
                stopForeground()
            }
        }
        albumsPercentageUpdater.observe(this) {
            if (it in 0..100) {
                notifyChange(getString(R.string.update_albums, it))
            } else {
                stopForeground()
            }
        }
        playlistsPercentageUpdater.observe(this) {
            if (it in 0..100) {
                notifyChange(getString(R.string.update_playlists, it))
            } else {
                stopForeground()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground()
        serviceScope.cancel()
    }

    private fun stopForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun notifyChange(message: String) {
        val notification = NotificationCompat.Builder(this, UPDATE_SESSION_NAME)
                .setContentTitle(message)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.notif)
                .setColor(ContextCompat.getColor(this, R.color.primary)).build()
        startForeground(2, notification)
    }

    companion object {
        const val UPDATE_SESSION_NAME = "AnyFlow updater"
    }
}