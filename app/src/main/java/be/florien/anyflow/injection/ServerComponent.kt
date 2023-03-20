package be.florien.anyflow.injection

import be.florien.anyflow.AnyFlowApp
import be.florien.anyflow.data.SyncService
import be.florien.anyflow.extension.MyAppGlideModule
import be.florien.anyflow.feature.alarms.AlarmViewModel
import be.florien.anyflow.feature.alarms.add.AddAlarmViewModel
import be.florien.anyflow.feature.alarms.edit.EditAlarmViewModel
import be.florien.anyflow.feature.alarms.list.AlarmListViewModel
import be.florien.anyflow.feature.connect.UserConnectViewModel
import be.florien.anyflow.feature.playlist.list.PlaylistListViewModel
import be.florien.anyflow.feature.playlist.songs.PlaylistSongsViewModel
import be.florien.anyflow.player.PlayerService
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

/**
 * Component used to add dependency injection about data into classes
 */
@ServerScope
@Subcomponent(modules = [ServerModule::class])
interface ServerComponent {

    fun inject(validator: AnyFlowApp.ServerValidator)

    fun inject(playerService: PlayerService)
    fun inject(syncService: SyncService)

    fun inject(userConnectViewModel: UserConnectViewModel)
    fun inject(viewModel: AlarmViewModel)
    fun inject(viewModel: AddAlarmViewModel)
    fun inject(viewModel: AlarmListViewModel)
    fun inject(viewModel: EditAlarmViewModel)
    fun inject(viewModel: PlaylistSongsViewModel)
    fun inject(viewModel: PlaylistListViewModel)

    fun inject(myAppGlideModule: MyAppGlideModule)

    fun playerComponentBuilder(): PlayerComponent.Builder
    fun quickActionsComponentBuilder(): QuickActionsComponent.Builder

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun ampacheUrl(@Named("serverUrl") ampacheUrl: String): Builder

        fun build(): ServerComponent
    }
}