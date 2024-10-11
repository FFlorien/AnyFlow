package be.florien.anyflow.injection

import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.common.ui.di.GlideModuleInjector
import be.florien.anyflow.data.server.di.ServerModule
import be.florien.anyflow.feature.alarm.ui.di.AlarmActivityComponent
import be.florien.anyflow.feature.alarm.ui.di.AlarmViewModelModule
import be.florien.anyflow.feature.auth.domain.di.AuthModule
import be.florien.anyflow.feature.filter.current.ui.di.CurrentFilterViewModelModule
import be.florien.anyflow.feature.library.podcast.ui.di.PodcastViewModelModule
import be.florien.anyflow.feature.library.tags.ui.di.LibraryViewModelModule
import be.florien.anyflow.feature.player.service.di.PlayerServiceComponent
import be.florien.anyflow.feature.playlist.di.PlaylistComponent
import be.florien.anyflow.feature.playlist.selection.ui.di.SelectPlaylistViewModelModule
import be.florien.anyflow.feature.shortcut.ui.di.ShortcutActivityComponent
import be.florien.anyflow.feature.sync.service.di.SyncServiceComponent
import be.florien.anyflow.feature.sync.service.di.SyncServiceModule
import be.florien.anyflow.management.playlist.di.PlaylistModificationWorkerModule
import be.florien.anyflow.ui.di.UserVmInjector
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

/**
 * Component used to add dependency injection about data into classes
 */
@ServerScope
@Subcomponent(
    modules = [
        ConnectedModule::class,
        ServerModule::class,
        SyncServiceModule::class,
        AuthModule::class,
        ServerBindsModule::class,
        LibraryViewModelModule::class,
        PodcastViewModelModule::class,
        SelectPlaylistViewModelModule::class,
        CurrentFilterViewModelModule::class,
        ViewModelModule::class,
        PlaylistModificationWorkerModule::class,
        AlarmViewModelModule::class
    ]
)
interface ServerComponent : UserVmInjector, GlideModuleInjector {

    fun inject(validator: AnyFlowApp.ServerValidator)

    fun playerServiceComponentBuilder(): PlayerServiceComponent.Builder
    fun playerComponentBuilder(): PlayerComponent.Builder
    fun shortcutsComponentBuilder(): ShortcutActivityComponent.Builder
    fun playlistComponentBuilder(): PlaylistComponent.Builder
    fun alarmComponentBuilder(): AlarmActivityComponent.Builder
    fun syncComponentBuilder(): SyncServiceComponent.Builder

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun ampacheUrl(@Named("serverUrl") ampacheUrl: String): Builder

        fun build(): ServerComponent
    }
}