package be.florien.ampacheplayer

import android.annotation.SuppressLint
import android.app.Application
import be.florien.ampacheplayer.api.AmpacheApi
import be.florien.ampacheplayer.api.AmpacheConnection
import be.florien.ampacheplayer.user.UserComponent
import io.realm.Realm
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class used for initialization of many libraries
 */
@SuppressLint("Registered")
open class AmpacheApp : Application() {
    lateinit var applicationComponent: ApplicationComponent
        protected set
    var userComponent: UserComponent? = null

    @Inject
    lateinit var ampacheConnection: AmpacheConnection
    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        Timber.plant(Timber.DebugTree())
        initApplicationComponent()
        ampacheConnection.ensureConnection()
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
}