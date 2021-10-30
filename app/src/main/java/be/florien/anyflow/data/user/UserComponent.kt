package be.florien.anyflow.data.user

import be.florien.anyflow.data.AmpacheDownloadService
import be.florien.anyflow.data.PingService
import be.florien.anyflow.data.UpdateService
import be.florien.anyflow.data.server.AmpacheApi
import be.florien.anyflow.injection.PlayerComponent
import be.florien.anyflow.injection.UserScope
import be.florien.anyflow.player.PlayerService
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * Component used to add dependency injection about data into classes
 */
@UserScope
@Subcomponent(modules = [UserModule::class])
interface UserComponent {

    fun inject(playerService: PlayerService)
    fun inject(updateService: UpdateService)
    fun inject(pingService: PingService)
    fun inject(downloadService: AmpacheDownloadService)

    fun playerComponentBuilder(): PlayerComponent.Builder

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun ampacheApi(ampacheApi: AmpacheApi): Builder

        fun build(): UserComponent
    }
}