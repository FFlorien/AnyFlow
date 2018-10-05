package be.florien.anyflow

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.support.multidex.MultiDexApplication
import be.florien.anyflow.di.DaggerApplicationComponent
import be.florien.anyflow.persistence.server.AmpacheApi
import be.florien.anyflow.persistence.server.AmpacheConnection
import be.florien.anyflow.user.UserComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import timber.log.Timber
import javax.inject.Inject
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric


/**
 * Application class used for initialization of many libraries
 */
@SuppressLint("Registered")
open class AnyFlowApp : MultiDexApplication() {
    lateinit var applicationComponent: be.florien.anyflow.di.ApplicationComponent
        protected set
    var userComponent: UserComponent? = null

    @Inject
    lateinit var ampacheConnection: AmpacheConnection
    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        Timber.plant(CrashReportingTree())
        initApplicationComponent()
        ampacheConnection.ensureConnection()
        createNotificationChannel()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> Timber.e(e, "Unexpected error") }
    }

    protected open fun initApplicationComponent() {
        applicationComponent = DaggerApplicationComponent
                .builder()
                .application(this)
                .build()
        applicationComponent.inject(this)
    }

    open fun createUserScopeForServer(serverUrl: String): AmpacheApi {
        val ampacheApi = Retrofit
                .Builder()
                .baseUrl(serverUrl)
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .build()
                .create(AmpacheApi::class.java)
        userComponent = applicationComponent
                //todo don't generate a new one if it is the same server
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
            notificationManager.createNotificationChannel(channel)
        }
    }
}