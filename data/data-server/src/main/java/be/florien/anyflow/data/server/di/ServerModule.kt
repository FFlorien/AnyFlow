package be.florien.anyflow.data.server.di

import android.content.Context
import androidx.work.WorkManager
import be.florien.anyflow.common.di.ServerScope
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import javax.inject.Named

@Module
class ServerModule {

    @Provides
    @ServerScope
    @Named("nonAuthenticated")
    fun providesAuthRetrofit(
        @Named("serverUrl") serverUrl: String,
        @Named("nonAuthenticated") okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder().baseUrl(serverUrl).client(okHttpClient)
        .addConverterFactory(JacksonConverterFactory.create()).build()

    @Provides
    @ServerScope
    @Named("authenticated")
    fun providesDataRetrofit(
        @Named("serverUrl") serverUrl: String,
        @Named("authenticated") okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder().baseUrl(serverUrl).client(okHttpClient)
        .addConverterFactory(JacksonConverterFactory.create()).build()

    @Provides
    @ServerScope
    fun providesWorkManager(
        context: Context
    ): WorkManager = WorkManager.getInstance(context)
}