package be.florien.anyflow.injection

import androidx.lifecycle.LiveData
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.sync.SyncRepository
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
    @Named("Songs")
    @ServerScope
    fun provideSongsPercentageUpdater(syncRepository: SyncRepository): LiveData<Int> =
        syncRepository.songsPercentageUpdater

    @Provides
    @Named("Genres")
    @ServerScope
    fun provideGenresPercentageUpdater(syncRepository: SyncRepository): LiveData<Int> =
        syncRepository.genresPercentageUpdater

    @Provides
    @Named("Artists")
    @ServerScope
    fun provideArtistsPercentageUpdater(syncRepository: SyncRepository): LiveData<Int> =
        syncRepository.artistsPercentageUpdater

    @Provides
    @Named("Albums")
    @ServerScope
    fun provideAlbumsPercentageUpdater(syncRepository: SyncRepository): LiveData<Int> =
        syncRepository.albumsPercentageUpdater

    @Provides
    @Named("Playlists")
    @ServerScope
    fun providePlaylistsPercentageUpdater(syncRepository: SyncRepository): LiveData<Int> =
        syncRepository.playlistsPercentageUpdater
}