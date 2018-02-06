package be.florien.ampacheplayer

import android.content.Context
import android.content.SharedPreferences
import be.florien.ampacheplayer.api.AmpacheApi
import be.florien.ampacheplayer.api.AmpacheConnection
import be.florien.ampacheplayer.api.MockUpAmpacheApi
import be.florien.ampacheplayer.user.AuthPersistence
import dagger.Module
import dagger.Provides
import dagger.Reusable
import io.realm.Realm
import javax.inject.Singleton

/**
 * todo
 */
@Module
class MockApplicationModule {

    companion object {
        private const val PREFERENCE_NAME = "ampache_preferences_Mockup"
    }

    @Provides
    @Singleton
    fun provideAmpacheApi(): AmpacheApi = MockUpAmpacheApi()

    @Provides
    @Reusable
    fun providePreferences(context: Context): SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideRealmRead(): Realm = Realm.getDefaultInstance()

    @Provides
    @Singleton
    fun provideAmpacheConnection(authPersistence: AuthPersistence, context: Context): AmpacheConnection = AmpacheConnection(authPersistence, context)

    @Provides
    @Singleton
    fun provideAuthManager(preference: SharedPreferences, context: Context): AuthPersistence = AuthPersistence(preference, context)
}