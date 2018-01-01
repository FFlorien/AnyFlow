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
import javax.inject.Singleton

/**
 * todo
 */
@Module
abstract class ApplicationModule {

    companion object {
        private const val PREFERENCE_NAME = "ampache_preferences"
    }

    @Provides
    @Singleton
    fun provideAmpacheApi(): AmpacheApi = Retrofit
            .Builder()
            .baseUrl("http://192.168.1.42/ampache/")
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
    fun provideAmpacheConnection(ampacheApi: AmpacheApi, authManager: AuthManager, context: Context): AmpacheConnection = AmpacheConnection(ampacheApi, authManager)

    @Provides
    @Singleton
    fun provideAuthManager(preference: SharedPreferences, context: Context): AuthManager = AuthManager(preference, context)
}