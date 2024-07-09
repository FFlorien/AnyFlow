package be.florien.anyflow.injection

import be.florien.anyflow.feature.player.services.queue.QueueRepository
import be.florien.anyflow.management.filters.FiltersRepository
import dagger.Binds
import dagger.Module

@Module
abstract class ServerBindsModule {

    @Binds
    abstract fun bindFiltersRepository(queueRepository: QueueRepository): FiltersRepository
}