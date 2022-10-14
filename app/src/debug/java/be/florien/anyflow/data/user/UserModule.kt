package be.florien.anyflow.data.user

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.LiveData
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.injection.UserScope
import be.florien.anyflow.player.*
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
    fun providePlayerController(
        context: Context,
        playingQueue: PlayingQueue,
        ampacheDataSource: AmpacheDataSource,
        filtersManager: FiltersManager,
        audioManager: AudioManager,
        alarmsSynchronizer: AlarmsSynchronizer,
        downSampleRepository: DownSampleRepository,
        cache: Cache,
        okHttpClient: OkHttpClient
    ): PlayerController = ExoPlayerController(
        playingQueue,
        ampacheDataSource,
        filtersManager,
        audioManager,
        alarmsSynchronizer,
        downSampleRepository,
        context,
        cache,
        okHttpClient
    )

    @Provides
    @UserScope
    fun provideDownSampleRepository(libraryDatabase: LibraryDatabase) = DownSampleRepository(libraryDatabase)

    @Provides
    @Named("Songs")
    @UserScope
    fun provideSongsPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.songsPercentageUpdater

    @Provides
    @Named("Genres")
    @UserScope
    fun provideGenresPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.genresPercentageUpdater

    @Provides
    @Named("Artists")
    @UserScope
    fun provideArtistsPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.artistsPercentageUpdater

    @Provides
    @Named("Albums")
    @UserScope
    fun provideAlbumsPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.albumsPercentageUpdater

    @Provides
    @Named("Playlists")
    @UserScope
    fun providePlaylistsPercentageUpdater(ampacheDataSource: AmpacheDataSource): LiveData<Int> =
        ampacheDataSource.playlistsPercentageUpdater
}