package be.florien.anyflow.injection

import android.content.Context
import android.content.SharedPreferences
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.user.AuthPersistence
import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
import dagger.Reusable
import io.reactivex.Observable
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Provide elements used through all the application state
 */
@Module
class ApplicationModule {

    companion object {
        private const val PREFERENCE_NAME = "anyflow_preferences"
    }

    @Singleton
    @Provides
    fun providePreferences(context: Context): SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

    @Singleton
    @Provides
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor()).build()

    @Singleton
    @Provides
    fun provideAmpacheConnection(authPersistence: AuthPersistence, context: Context, sharedPreferences: SharedPreferences): AmpacheConnection = AmpacheConnection(authPersistence, context, sharedPreferences)

    @Provides
    fun provideAmpacheConnectionStatus(connection: AmpacheConnection): Observable<AmpacheConnection.ConnectionStatus> = connection.connectionStatusUpdater

    @Singleton
    @Provides
    fun provideLibrary(context: Context): LibraryDatabase = LibraryDatabase.getInstance(context)
}