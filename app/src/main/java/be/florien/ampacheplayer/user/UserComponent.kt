package be.florien.ampacheplayer.user

import be.florien.ampacheplayer.ApplicationComponent
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.player.PlayerComponent
import dagger.BindsInstance
import dagger.Component


/**
 * Component used to add dependency injection about data into classes
 */
@UserScope
@Component(dependencies = [(ApplicationComponent::class)], modules = [(UserModule::class)])
interface UserComponent {

    fun inject(playerService: PlayerService)

    fun playerComponentBuilder(): PlayerComponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun ampacheUrl(ampacheUrl: String): Builder
        fun applicationComponent(applicationComponent: ApplicationComponent): Builder

        fun build(): UserComponent
    }

}