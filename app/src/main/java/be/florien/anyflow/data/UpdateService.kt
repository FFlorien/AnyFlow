package be.florien.anyflow.data

import android.app.PendingIntent
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import be.florien.anyflow.CrashReportingTree
import be.florien.anyflow.R
import be.florien.anyflow.data.server.exception.NoServerException
import be.florien.anyflow.data.server.exception.SessionExpiredException
import be.florien.anyflow.data.server.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.extension.iLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Named

class UpdateService
@Inject constructor() : LifecycleService() {
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(serviceJob)

    @Inject
    lateinit var dataRepository: DataRepository

    @Inject
    @field:Named("Songs")
    lateinit var songsPercentageUpdater: LiveData<Int>

    @Inject
    @field:Named("Albums")
    lateinit var albumsPercentageUpdater: LiveData<Int>

    @Inject
    @field:Named("Artists")
    lateinit var artistsPercentageUpdater: LiveData<Int>
    private val pendingIntent: PendingIntent by lazy {
        val intent = packageManager?.getLaunchIntentForPackage(packageName)
        PendingIntent.getActivity(this@UpdateService, 0, intent, 0)
    }

    override fun onCreate() {
        super.onCreate()
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
        Timber.plant(CrashReportingTree())
        serviceScope.launch {
            try {
                dataRepository.updateAll()
            } catch (throwable: Throwable) {
                when (throwable) {
                    is SessionExpiredException ->
                        this@UpdateService.iLog(throwable, "The session token is expired")
                    is WrongIdentificationPairException ->
                        this@UpdateService.iLog(throwable, "Couldn't reconnect the user: wrong user/pwd")
                    is SocketTimeoutException, is NoServerException ->
                        this@UpdateService.eLog(throwable, "Couldn't connect to the webservice")
                    else ->
                        this@UpdateService.eLog(throwable, "Unknown error")
                }
            }
            stopForeground(true)
        }
        songsPercentageUpdater.observe(this, {
            if (it in 0..100) {
                notifyChange(getString(R.string.update_songs, it))
            } else {
                stopForeground(true)
            }

        })
        artistsPercentageUpdater.observe(this, {
            if (it in 0..100) {
                notifyChange(getString(R.string.update_artists, it))
            } else {
                stopForeground(true)
            }
        })
        albumsPercentageUpdater.observe(this, {
            if (it in 0..100) {
                notifyChange(getString(R.string.update_albums, it))
            } else {
                stopForeground(true)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
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