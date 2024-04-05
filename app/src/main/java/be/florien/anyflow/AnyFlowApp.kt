package be.florien.anyflow

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.multidex.MultiDexApplication
import be.florien.anyflow.data.user.AuthPersistence
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.feature.auth.AuthRepository
import be.florien.anyflow.feature.sync.SyncService
import be.florien.anyflow.injection.ApplicationComponent
import be.florien.anyflow.injection.DaggerApplicationComponent
import be.florien.anyflow.injection.ServerComponent
import fr.bipi.treessence.file.FileLoggerTree
import timber.log.Timber
import java.io.File
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
        val logDirectory = File(filesDir.absolutePath + "/logs")
        if (!logDirectory.exists()) {
            logDirectory.mkdir()
        }
        Timber.plant(
            FileLoggerTree
                .Builder()
                .withDir(logDirectory)
                .withFileName("anyflow_log_%g.log")
                .withFileLimit(5)
                .withMinPriority(Log.DEBUG)
                .appendToFile(true)
                .build()
        )
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
            val updateChannel = getUpdateChannel()
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(updateChannel)
        }
    }

    private fun getUpdateChannel(): NotificationChannel {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SyncService.UPDATE_SESSION_NAME,
                "Update",
                NotificationManager.IMPORTANCE_DEFAULT
            )
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