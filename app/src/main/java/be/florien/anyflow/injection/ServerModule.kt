package be.florien.anyflow.injection

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.LiveData
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.data.local.LibraryDatabase
import be.florien.anyflow.data.server.AmpacheAuthSource
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.feature.alarms.AlarmsSynchronizer
import be.florien.anyflow.player.*
import com.google.android.exoplayer2.upstream.cache.Cache
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import javax.inject.Named

/**
 * Module for dependencies available only when a user is logged in.
 */
@Module
class ServerModule {

    @Provides
    @ServerScope
    fun providesRetrofit(
        @Named("serverUrl") serverUrl: String,
        okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder().baseUrl(serverUrl).client(okHttpClient)
        .addConverterFactory(JacksonConverterFactory.create()).build()

    @Provides
    @ServerScope
    fun providePlayerController(
        context: Context,
        playingQueue: PlayingQueue,
        ampacheAuthSource: AmpacheAuthSource,
        ampacheDataSource: AmpacheDataSource,
        filtersManager: FiltersManager,
        audioManager: AudioManager,
        downloadManager: DownloadManager,
        alarmsSynchronizer: AlarmsSynchronizer,
        cache: Cache,
        okHttpClient: OkHttpClient
    ): PlayerController = ExoPlayerController(
        playingQueue,
        ampacheAuthSource,
        ampacheDataSource,
        filtersManager,
        audioManager,
        downloadManager,
        alarmsSynchronizer,
        context,
        cache,
        okHttpClient
    )

    @Provides
    fun provideAlarmsSynchronizer(
        alarmManager: AlarmManager,
        dataRepository: DataRepository,
        @Named("player") playerIntent: PendingIntent,
        @Named("alarm") alarmIntent: PendingIntent
    ): AlarmsSynchronizer =
        AlarmsSynchronizer(alarmManager, dataRepository, alarmIntent, playerIntent)

    @Provides
    fun provideConnectionStatus(connection: AmpacheAuthSource): LiveData<AmpacheAuthSource.ConnectionStatus> =
        connection.connectionStatusUpdater

    @Provides
    @ServerScope
    fun provideWaveFormRepository(
        libraryDatabase: LibraryDatabase,
        ampacheDataSource: AmpacheDataSource,
        context: Context
    ) = WaveFormRepository(libraryDatabase, ampacheDataSource, context)

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