package be.florien.anyflow

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.multidex.MultiDexApplication
import be.florien.anyflow.data.AuthRepository
import be.florien.anyflow.data.SyncService
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.injection.ServerComponent
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ApplicationComponent
import be.florien.anyflow.injection.DaggerApplicationComponent
import be.florien.anyflow.player.PlayerService
import timber.log.Timber
import javax.inject.Inject


/**
 * Application class used for initialization of many libraries
 */
@SuppressLint("Registered")
open class AnyFlowApp : MultiDexApplication(), ServerComponentContainer {
    lateinit var applicationComponent: ApplicationComponent
        protected set
    override var serverComponent: ServerComponent? = null

    @Inject
    lateinit var authPersistence: AuthPersistence

    override fun onCreate() {
        super.onCreate()
        Timber.plant(CrashReportingTree())
        initApplicationComponent()
        initServerComponentIfReady()
        createNotificationChannels()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            this@AnyFlowApp.eLog(e, "Unexpected error")
        }
    }

    protected open fun initApplicationComponent() {
        applicationComponent = DaggerApplicationComponent
                .builder()
                .application(this)
                .build()
        applicationComponent.inject(this)
    }

    private fun initServerComponentIfReady() {
        val serverUrl = authPersistence.serverUrl
        if (serverUrl.hasSecret()) {
            createServerComponent(serverUrl.secret)
        }
    }

    override suspend fun createServerComponentIfServerValid(serverUrl: String): Boolean {
        createServerComponent(serverUrl)

        val validator = ServerValidator()
        serverComponent?.inject(validator)

        return if (validator.isServerValid()) {
            true
        } else {
            serverComponent = null
            false
        }
    }

    private fun createServerComponent(serverUrl: String) {
        serverComponent = applicationComponent
            .serverComponentBuilder()
            .ampacheUrl(serverUrl)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playerChannel = getPlayerChannel()
            val updateChannel = getUpdateChannel()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(playerChannel)
            notificationManager?.createNotificationChannel(updateChannel)
        }
    }

    private fun getPlayerChannel(): NotificationChannel {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(PlayerService.MEDIA_SESSION_NAME, "Music", NotificationManager.IMPORTANCE_LOW)
            channel.description = "It play music"
            channel.vibrationPattern = null
            channel.enableVibration(false)
            return channel
        } else {
            throw UnsupportedOperationException("This method shouldn't be called from this api")
        }
    }

    private fun getUpdateChannel(): NotificationChannel {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(SyncService.UPDATE_SESSION_NAME, "Update", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "It update your music database"
            return channel
        } else {
            throw UnsupportedOperationException("This method shouldn't be called from this api")
        }
    }

    class ServerValidator {
        @Inject
        lateinit var authRepository: AuthRepository

        suspend fun isServerValid() = authRepository.ping()
    }
}