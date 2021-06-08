package be.florien.anyflow

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.multidex.MultiDexApplication
import be.florien.anyflow.data.server.AmpacheApi
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.user.UserComponent
import be.florien.anyflow.extension.eLog
import be.florien.anyflow.injection.ApplicationComponent
import be.florien.anyflow.injection.DaggerApplicationComponent
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
    lateinit var ampacheConnection: AmpacheConnection

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        Timber.plant(CrashReportingTree())
        initApplicationComponent()
        ampacheConnection.ensureConnection()
        createNotificationChannel()
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            this@AnyFlowApp.eLog(
                e,
                "Unexpected error"
            )
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music"
            val description = "It play music"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("AnyFlow", name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}