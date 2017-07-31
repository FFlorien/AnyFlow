package be.florien.ampacheplayer.di

import android.content.Context
import android.content.SharedPreferences
import be.florien.ampacheplayer.manager.*
import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
import io.realm.Realm
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import javax.inject.Singleton

/**
 * Module for all things ampache data related
 */
@Module
class DataModule {

    @Singleton
    @Provides
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .addNetworkInterceptor(StethoInterceptor())
            .build()

    @Singleton
    @Provides
    fun provideAmpacheApi(okHttpClient: OkHttpClient): AmpacheApi {
        val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.1.42/ampache/")
                .addConverterFactory(SimpleXmlConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(okHttpClient)
                .build()

        return retrofit.create(AmpacheApi::class.java)
    }

    @Singleton
    @Provides
    fun provideAmpacheConnection(ampacheApi: AmpacheApi, authManager: AuthManager, context: Context): AmpacheConnection = AmpacheConnection(ampacheApi, authManager, context)

    @Singleton
    @Provides
    fun provideRealmRead(): Realm = Realm.getDefaultInstance()


    @Singleton
    @Provides
    fun provideAmpacheDatabase(realmRead: Realm): DatabaseManager = DatabaseManager(realmRead)

    @Singleton
    @Provides
    fun providePersistenceManager(databaseManager: DatabaseManager, connection: AmpacheConnection): PersistenceManager = PersistenceManager(databaseManager, connection)

    @Singleton
    @Provides
    fun provideAudioQueueManager(databaseManager: DatabaseManager) : AudioQueueManager = AudioQueueManager(databaseManager)

    @Singleton
    @Provides
    fun provideAuthenticationManager(context: Context, prefs: SharedPreferences): AuthManager = AuthManager(prefs, context)
}