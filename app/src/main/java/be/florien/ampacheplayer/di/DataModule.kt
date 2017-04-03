package be.florien.ampacheplayer.di

import be.florien.ampacheplayer.manager.*
import com.facebook.stetho.okhttp3.StethoInterceptor
import dagger.Module
import dagger.Provides
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

    @Provides
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
                .addNetworkInterceptor(StethoInterceptor())
                .build()
    }

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
    fun provideAmpacheConnection(): AmpacheConnection {
        return AmpacheConnection()
    }

    @Provides
    fun provideAmpacheDatabase(): AmpacheDatabase {
        return AmpacheDatabase()
    }

    @Provides
    fun provideDataManager(): DataManager {
        return DataManager()
    }

    @Provides
    fun provideAuthenticationManager(): AuthenticationManager {
        return AuthenticationManager()
    }
}