package be.florien.anyflow.management.playlist.di

import be.florien.anyflow.common.di.ActivityScope
import be.florien.anyflow.management.playlist.work.PlaylistModificationWorker
import dagger.Subcomponent

@Subcomponent
@ActivityScope
interface PlaylistModificationWorkerComponent {
    fun inject(playlistModificationWorker: PlaylistModificationWorker)

    @Subcomponent.Builder
    interface Builder {

        fun build(): PlaylistModificationWorkerComponent
    }

}
