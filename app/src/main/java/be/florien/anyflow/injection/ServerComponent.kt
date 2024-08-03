package be.florien.anyflow.injection

import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.architecture.di.ServerScope
import be.florien.anyflow.common.ui.di.GlideModuleInjector
import be.florien.anyflow.data.server.di.ServerModule
import be.florien.anyflow.feature.alarms.AlarmViewModel
import be.florien.anyflow.feature.alarms.add.AddAlarmViewModel
import be.florien.anyflow.feature.alarms.edit.EditAlarmViewModel
import be.florien.anyflow.feature.alarms.list.AlarmListViewModel
import be.florien.anyflow.feature.auth.domain.di.AuthModule
import be.florien.anyflow.feature.library.ui.di.LibraryViewModelModule
import be.florien.anyflow.feature.player.services.PlayerService
import be.florien.anyflow.feature.playlist.list.PlaylistListViewModel
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsViewModel
import be.florien.anyflow.feature.sync.SyncService
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
        AuthModule::class,
        ServerBindsModule::class,
        LibraryViewModelModule::class,
        ViewModelModule::class
    ]
)
interface ServerComponent : UserVmInjector, GlideModuleInjector {

    fun inject(validator: AnyFlowApp.ServerValidator)

    fun inject(playerService: PlayerService)
    fun inject(syncService: SyncService)

    fun inject(viewModel: AlarmViewModel)
    fun inject(viewModel: AddAlarmViewModel)
    fun inject(viewModel: AlarmListViewModel)
    fun inject(viewModel: EditAlarmViewModel)
    fun inject(viewModel: PlaylistSongsViewModel)
    fun inject(viewModel: PlaylistListViewModel)

    fun playerComponentBuilder(): PlayerComponent.Builder
    fun shortcutsComponentBuilder(): ShortcutsComponent.Builder

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun ampacheUrl(@Named("serverUrl") ampacheUrl: String): Builder

        fun build(): ServerComponent
    }
}