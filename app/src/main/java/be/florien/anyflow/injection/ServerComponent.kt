package be.florien.anyflow.injection

import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.ui.di.GlideModuleInjector
import be.florien.anyflow.data.server.di.ServerModule
import be.florien.anyflow.feature.alarm.ui.di.AlarmActivityComponent
import be.florien.anyflow.feature.auth.domain.di.AuthModule
import be.florien.anyflow.feature.auth.ui.di.UserConnectActivityComponent
import be.florien.anyflow.feature.filter.current.ui.di.CurrentFilterViewModelModule
import be.florien.anyflow.feature.filter.saved.ui.di.SavedFilterGroupViewModelModule
import be.florien.anyflow.feature.library.podcast.ui.di.PodcastViewModelModule
import be.florien.anyflow.feature.library.tags.ui.di.LibraryViewModelModule
import be.florien.anyflow.feature.player.service.di.PlayerServiceComponent
import be.florien.anyflow.feature.player.ui.di.PlayerActivityComponent
import be.florien.anyflow.feature.playlist.di.PlaylistActivityComponent
import be.florien.anyflow.feature.playlist.selection.ui.di.SelectPlaylistViewModelModule
import be.florien.anyflow.feature.shortcut.ui.di.ShortcutActivityComponent
import be.florien.anyflow.feature.song.ui.di.SongInfoViewModelModule
import be.florien.anyflow.feature.songlist.ui.di.SongListViewModelModule
import be.florien.anyflow.feature.sync.service.di.SyncServiceComponent
import be.florien.anyflow.feature.sync.service.di.SyncServiceModule
import be.florien.anyflow.management.playlist.di.PlaylistModificationWorkerModule
import be.florien.anyflow.management.queue.di.QueueModule
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

/**
 * Component for when the user has selected a server
 */
@ServerScope
@Subcomponent(
    modules = [
        // ViewModelModules
        SongListViewModelModule::class,
        SongInfoViewModelModule::class,
        LibraryViewModelModule::class,
        PodcastViewModelModule::class,
        CurrentFilterViewModelModule::class,
        SavedFilterGroupViewModelModule::class,
        SelectPlaylistViewModelModule::class,
        // ProvideModules
        ConnectedModule::class,
        ServerModule::class,
        // ProvideModules from gradle modules
        SyncServiceModule::class,
        QueueModule::class,
        AuthModule::class,
        PlaylistModificationWorkerModule::class
    ]
)
interface ServerComponent : GlideModuleInjector {
    // Services
    fun playerServiceComponentBuilder(): PlayerServiceComponent.Builder
    fun syncServiceComponentBuilder(): SyncServiceComponent.Builder
    // Activities
    fun userConnectComponentBuilder(): UserConnectActivityComponent.Builder
    fun playerComponentBuilder(): PlayerActivityComponent.Builder
    fun alarmComponentBuilder(): AlarmActivityComponent.Builder
    fun playlistComponentBuilder(): PlaylistActivityComponent.Builder
    fun shortcutsComponentBuilder(): ShortcutActivityComponent.Builder

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun ampacheUrl(@Named("serverUrl") ampacheUrl: String): Builder

        fun build(): ServerComponent
    }
}