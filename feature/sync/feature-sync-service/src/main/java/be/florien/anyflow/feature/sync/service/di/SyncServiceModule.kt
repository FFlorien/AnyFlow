package be.florien.anyflow.feature.sync.service.di

import androidx.lifecycle.LiveData
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.feature.sync.service.SyncRepository
import dagger.Module
import dagger.Provides

@Module
class SyncServiceModule {

    @Provides
    @ServerScope
    fun provideSongsPercentageUpdater(syncRepository: SyncRepository): LiveData<SyncRepository.PercentageUpdate> =
        syncRepository.libraryPercentageUpdater
}