package be.florien.ampacheplayer

import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.player.PlayerActivity
import be.florien.ampacheplayer.view.player.filter.FilterFragment
import be.florien.ampacheplayer.view.player.songlist.SongListFragment
import dagger.BindsInstance
import dagger.Component


/**
 * Component used to add dependency injection about data into classes
 */
@UserScope
@Component(dependencies = [(ApplicationComponent::class)], modules = [(UserModule::class)])
interface UserComponent {

    fun inject(playerService: PlayerService)
    fun inject(playerActivity: PlayerActivity)
    fun inject(songListFragment: SongListFragment)
    fun inject(filterFragment: FilterFragment)

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun ampacheUrl(ampacheUrl: String): Builder
        fun applicationComponent(applicationComponent: ApplicationComponent): Builder

        fun build(): UserComponent
    }

}