package be.florien.anyflow.feature.sync.service.di

import be.florien.anyflow.feature.sync.service.SyncService
import dagger.Subcomponent

@Subcomponent
interface SyncServiceComponent {

    fun inject(service: SyncService)

    @Subcomponent.Builder
    interface Builder {

        fun build(): SyncServiceComponent
    }
}