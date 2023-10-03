package be.florien.anyflow.injection

import androidx.lifecycle.LiveData
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.data.server.AuthenticationInterceptor
import be.florien.anyflow.feature.auth.AuthRepository
import be.florien.anyflow.feature.player.services.controller.ExoPlayerController
import be.florien.anyflow.feature.player.services.controller.PlayerController
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named

/**
 * Module for dependencies available only when a user is logged in.
 */
@Module
class ServerModule {

    @ServerScope
    @Provides
    @Named("nonAuthenticated")
    fun provideAuthOkHttp(): OkHttpClient =
        OkHttpClient.Builder().build()

    @ServerScope
    @Provides
    @Named("authenticated")
    fun provideDataOkHttp(authenticationInterceptor: AuthenticationInterceptor): OkHttpClient =
        OkHttpClient.Builder().addInterceptor(authenticationInterceptor).build()

    @ServerScope
    @Provides
    @Named("glide")
    fun provideGlideOkHttp(authenticationInterceptor: AuthenticationInterceptor): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(authenticationInterceptor)
            .callTimeout(60, TimeUnit.SECONDS)//it may need some time to generate those
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

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
    fun bindsPlayerController(exoPlayerController: ExoPlayerController): PlayerController =
        exoPlayerController

    @Provides
    fun provideConnectionStatus(connection: AuthRepository): LiveData<AuthRepository.ConnectionStatus> =
        connection.connectionStatusUpdater

    @Provides
    @Named("Songs")
    @ServerScope
    fun provideSongsPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.songsPercentageUpdater

    @Provides
    @Named("Genres")
    @ServerScope
    fun provideGenresPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.genresPercentageUpdater

    @Provides
    @Named("Artists")
    @ServerScope
    fun provideArtistsPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.artistsPercentageUpdater

    @Provides
    @Named("Albums")
    @ServerScope
    fun provideAlbumsPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.albumsPercentageUpdater

    @Provides
    @Named("Playlists")
    @ServerScope
    fun providePlaylistsPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.playlistsPercentageUpdater
}