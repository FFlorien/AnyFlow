package be.florien.anyflow.feature.sync.service.di

import androidx.lifecycle.LiveData
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.feature.sync.service.SyncRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class SyncServiceModule {

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