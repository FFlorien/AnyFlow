package be.florien.anyflow

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.multidex.MultiDexApplication
import be.florien.anyflow.data.SyncService
import be.florien.anyflow.data.server.AmpacheApi
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.user.UserComponent
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ApplicationComponent
import be.florien.anyflow.injection.DaggerApplicationComponent
import be.florien.anyflow.player.PlayerService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import timber.log.Timber
import javax.inject.Inject


/**
 * Application class used for initialization of many libraries
 */
@SuppressLint("Registered")
open class AnyFlowApp : MultiDexApplication(), UserComponentContainer {
    lateinit var applicationComponent: ApplicationComponent
        protected set
    override var userComponent: UserComponent? = null

    @Inject
    lateinit var ampacheDataSource: AmpacheDataSource

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        Timber.plant(CrashReportingTree())
        initApplicationComponent()
        ampacheDataSource.ensureConnection()
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

    override fun createUserScopeForServer(serverUrl: String): AmpacheApi {
        val ampacheApi = Retrofit
                .Builder()
                .baseUrl(serverUrl)
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(AmpacheApi::class.java)
        userComponent = applicationComponent
                .userComponentBuilder()
                .ampacheApi(ampacheApi)
                .build()
        return ampacheApi
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
}