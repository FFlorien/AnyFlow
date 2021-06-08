package be.florien.anyflow.data.user

import android.content.Context
import androidx.lifecycle.LiveData
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.injection.UserScope
import be.florien.anyflow.player.ExoPlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayingQueue
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import javax.inject.Named

/**
 * Module for dependencies available only when a user is logged in.
 */
@Module
class UserModule {

    @Provides
    @UserScope
    fun providePlayerController(context: Context, playingQueue: PlayingQueue, ampacheConnection: AmpacheConnection, okHttpClient: OkHttpClient): PlayerController = ExoPlayerController(playingQueue, ampacheConnection, context, okHttpClient)

    @Provides
    @Named("Songs")
    @UserScope
    fun provideSongsPercentageUpdater(ampacheConnection: AmpacheConnection): LiveData<Int> = ampacheConnection.songsPercentageUpdater

    @Provides
    @Named("Artists")
    @UserScope
    fun provideArtistsPercentageUpdater(ampacheConnection: AmpacheConnection): LiveData<Int> = ampacheConnection.artistsPercentageUpdater

    @Provides
    @Named("Albums")
    @UserScope
    fun provideAlbumsPercentageUpdater(ampacheConnection: AmpacheConnection): LiveData<Int> = ampacheConnection.albumsPercentageUpdater
}