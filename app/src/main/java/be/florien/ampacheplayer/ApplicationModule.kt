package be.florien.ampacheplayer

import android.content.Context
import android.content.SharedPreferences
import be.florien.ampacheplayer.api.AmpacheConnection
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
class ApplicationModule {

    companion object {
        private const val PREFERENCE_NAME = "ampache_preferences"
    }

    @Provides
    @Reusable
    fun providePreferences(context: Context): SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideRealm(): Realm = Realm.getDefaultInstance()

    @Singleton
    @Provides
    fun provideAmpacheConnection(authPersistence: AuthPersistence, context: Context): AmpacheConnection = AmpacheConnection(authPersistence, context)
}