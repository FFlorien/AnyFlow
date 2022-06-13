package be.florien.anyflow.data.user

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.LiveData
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.injection.UserScope
import be.florien.anyflow.player.ExoPlayerController
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.PlayerController
import be.florien.anyflow.player.PlayingQueue
import com.google.android.exoplayer2.upstream.cache.Cache
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
    fun providePlayerController(context: Context, playingQueue: PlayingQueue, ampacheConnection: AmpacheConnection, filtersManager: FiltersManager, audioManager: AudioManager, alarmsSynchronizer: AlarmsSynchronizer, cache: Cache, okHttpClient: OkHttpClient): PlayerController = ExoPlayerController(playingQueue, ampacheConnection, filtersManager, audioManager, alarmsSynchronizer, context, cache, okHttpClient)

    @Provides
    @Named("Songs")
    @UserScope
    fun provideSongsPercentageUpdater(ampacheConnection: AmpacheConnection): LiveData<Int> = ampacheConnection.songsPercentageUpdater

    @Provides
    @Named("Genres")
    @UserScope
    fun provideGenresPercentageUpdater(ampacheConnection: AmpacheConnection): LiveData<Int> = ampacheConnection.genresPercentageUpdater

    @Provides
    @Named("Artists")
    @UserScope
    fun provideArtistsPercentageUpdater(ampacheConnection: AmpacheConnection): LiveData<Int> = ampacheConnection.artistsPercentageUpdater

    @Provides
    @Named("Albums")
    @UserScope
    fun provideAlbumsPercentageUpdater(ampacheConnection: AmpacheConnection): LiveData<Int> = ampacheConnection.albumsPercentageUpdater

    @Provides
    @Named("Playlists")
    @UserScope
    fun providePlaylistsPercentageUpdater(ampacheConnection: AmpacheConnection): LiveData<Int> = ampacheConnection.playlistsPercentageUpdater
}