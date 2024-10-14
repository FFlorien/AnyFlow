package be.florien.anyflow.management.queue.di

import be.florien.anyflow.management.filters.FiltersRepository
import be.florien.anyflow.management.queue.QueueRepository
import dagger.Binds
import dagger.Module

@Module
abstract class QueueModule {

    @Binds
    abstract fun bindFiltersRepository(queueRepository: QueueRepository): FiltersRepository
}