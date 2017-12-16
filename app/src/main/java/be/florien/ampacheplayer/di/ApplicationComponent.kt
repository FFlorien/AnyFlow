package be.florien.ampacheplayer.di

import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.viewmodel.ConnectActivityVM
import be.florien.ampacheplayer.view.viewmodel.PlayerActivityVM
import be.florien.ampacheplayer.view.viewmodel.SongListFragmentVM
import dagger.Component
import javax.inject.Singleton

/**
 * Component used to add dependency injection about data into classes
 */
@Singleton
@Component(
        modules = arrayOf(
                DataModule::class,
                AndroidModule::class
        )
)
interface ApplicationComponent {
    fun inject(playerService: PlayerService)
    fun inject(connectActivityVM: ConnectActivityVM)
    fun inject(songListFragmentVM: SongListFragmentVM)
    fun inject(playerActivityVM: PlayerActivityVM)
    //todo unbind !!!
}