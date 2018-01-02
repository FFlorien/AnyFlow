package be.florien.ampacheplayer

import android.content.Context
import android.content.SharedPreferences
import be.florien.ampacheplayer.api.AmpacheApi
import be.florien.ampacheplayer.api.AmpacheConnection
import be.florien.ampacheplayer.persistence.AuthManager
import dagger.Module
import dagger.Provides
import dagger.Reusable
import io.realm.Realm
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import javax.inject.Singleton

/**
 * todo
 */
@Module
class ApplicationModule {

    companion object {
        private const val PREFERENCE_NAME = "ampache_preferences"
    }

    @Provides
    @Singleton
    fun provideAmpacheApi(): AmpacheApi = Retrofit
            .Builder()
            .baseUrl("http://192.168.1.42/ampache/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()
            .create(AmpacheApi::class.java)

    @Provides
    @Reusable
    fun providePreferences(context: Context): SharedPreferences = context.getSharedPreferences(ApplicationModule.PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideRealmRead(): Realm = Realm.getDefaultInstance()

    @Provides
    @Singleton
    fun provideAmpacheConnection(ampacheApi: AmpacheApi, authManager: AuthManager): AmpacheConnection = AmpacheConnection(ampacheApi, authManager)

    @Provides
    @Singleton
    fun provideAuthManager(preference: SharedPreferences, context: Context): AuthManager = AuthManager(preference, context)
}