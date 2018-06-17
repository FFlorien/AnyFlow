package be.florien.ampacheplayer

import android.content.Context
import android.content.SharedPreferences
import be.florien.ampacheplayer.persistence.server.AmpacheConnection
import be.florien.ampacheplayer.user.AuthPersistence
import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
import dagger.Reusable
import okhttp3.OkHttpClient
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
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor()).build()

    @Singleton
    @Provides
    fun provideAmpacheConnection(authPersistence: AuthPersistence, context: Context): AmpacheConnection = AmpacheConnection(authPersistence, context)
}