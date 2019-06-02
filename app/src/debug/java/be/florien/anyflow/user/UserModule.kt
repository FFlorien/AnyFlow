package be.florien.anyflow.user

import android.content.Context
import be.florien.anyflow.di.UserScope
import be.florien.anyflow.local.DownloadHelper
import be.florien.anyflow.persistence.local.LibraryDatabase
import be.florien.anyflow.persistence.server.AmpacheConnection
import be.florien.anyflow.player.ExoPlayerController
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayingQueue
import dagger.Module
import dagger.Provides
import io.reactivex.Observable
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
    @UserScope
    fun provideDownloadHelper(libraryDatabase: LibraryDatabase, ampacheConnection: AmpacheConnection, context: Context): DownloadHelper
            = DownloadHelper(libraryDatabase, ampacheConnection, context)

    @Provides
    @Named("Songs")
    @UserScope
    fun provideSongsPercentageUpdater(ampacheConnection: AmpacheConnection): Observable<Int> = ampacheConnection.songsPercentageUpdater

    @Provides
    @Named("Artists")
    @UserScope
    fun provideArtistsPercentageUpdater(ampacheConnection: AmpacheConnection): Observable<Int> = ampacheConnection.artistsPercentageUpdater

    @Provides
    @Named("Albums")
    @UserScope
    fun provideAlbumsPercentageUpdater(ampacheConnection: AmpacheConnection): Observable<Int> = ampacheConnection.albumsPercentageUpdater
}