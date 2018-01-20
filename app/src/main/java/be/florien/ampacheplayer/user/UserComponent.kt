package be.florien.ampacheplayer.user

import be.florien.ampacheplayer.api.AmpacheApi
import be.florien.ampacheplayer.di.UserScope
import be.florien.ampacheplayer.player.PlayerService
import be.florien.ampacheplayer.view.player.PlayerComponent
import dagger.BindsInstance
import dagger.Subcomponent


/**
 * Component used to add dependency injection about data into classes
 */
@UserScope
@Subcomponent(modules = [(UserModule::class)])
interface UserComponent {

    fun inject(playerService: PlayerService)

    fun playerComponentBuilder(): PlayerComponent.Builder

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun ampacheApi(ampacheApi: AmpacheApi): Builder

        fun build(): UserComponent
    }

}