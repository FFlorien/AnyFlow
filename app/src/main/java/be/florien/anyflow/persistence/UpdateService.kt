package be.florien.anyflow.persistence

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import be.florien.anyflow.R
import be.florien.anyflow.exception.NoServerException
import be.florien.anyflow.exception.SessionExpiredException
import be.florien.anyflow.exception.WrongIdentificationPairException
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.extension.iLog
import be.florien.anyflow.player.PlayerService
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.net.SocketTimeoutException
import javax.inject.Inject

class UpdateService
@Inject constructor() : Service() {
    override fun onBind(intent: Intent?): IBinder?  = null

    @Inject
    lateinit var persistenceManager: PersistenceManager
    private val pendingIntent: PendingIntent by lazy {
        val intent = packageManager?.getLaunchIntentForPackage(packageName)
        PendingIntent.getActivity(this@UpdateService, 0, intent, 0)
    }
    private var subscription: Disposable? = null

    override fun onCreate() {
        super.onCreate()
        (application as be.florien.anyflow.AnyFlowApp).userComponent?.inject(this)
        notifyChange(true)
        subscription = persistenceManager.updateAll()
                .doOnError { throwable ->
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
                .doOnComplete {
                    notifyChange(false)
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    override fun onDestroy() {
        super.onDestroy()
        notifyChange(false)
        subscription?.dispose()
    }

    private fun notifyChange(isUpdating: Boolean) {
        if (isUpdating) {
            val notification = NotificationCompat.Builder(this, PlayerService.MEDIA_SESSION_NAME)
                    .setContentTitle(getString(R.string.updating_library))
                    .setContentIntent(pendingIntent)
                    .setOnlyAlertOnce(true)
                    .setSmallIcon(R.drawable.notif)
                    .setColor(ContextCompat.getColor(this, R.color.primary)).build()
            startForeground(2, notification)
        } else {
            stopForeground(true)
        }
    }

}