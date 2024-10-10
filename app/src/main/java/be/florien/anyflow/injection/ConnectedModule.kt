package be.florien.anyflow.injection

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.player.ui.PlayerActivity
import be.florien.anyflow.feature.sync.service.SyncRepository
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named

/**
 * Module for dependencies available only when a user is logged in.
 */
@Module
class ConnectedModule {

    @ServerScope
    @Provides
    @Named("authenticated")
    fun provideDataOkHttp(authenticationInterceptor: AuthenticationInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(authenticationInterceptor)
            .build()

    @ServerScope
    @Provides
    @Named("glide")
    fun provideGlideOkHttp(authenticationInterceptor: AuthenticationInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authenticationInterceptor)
            .callTimeout(60, TimeUnit.SECONDS)//it may need some time to generate the waveform image
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    @Provides
    @Named("playerActivity")
    @ServerScope
    fun providePlayerActivityIntent(context: Context) = Intent(context, PlayerActivity::class.java)
}