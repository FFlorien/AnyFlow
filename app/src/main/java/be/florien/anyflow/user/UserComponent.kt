package be.florien.anyflow.user

import be.florien.anyflow.di.UserScope
import be.florien.anyflow.persistence.DownloadStatusService
import be.florien.anyflow.persistence.PingService
import be.florien.anyflow.persistence.UpdateService
import be.florien.anyflow.persistence.server.AmpacheApi
import be.florien.anyflow.player.PlayerService
import be.florien.anyflow.view.player.PlayerComponent
import dagger.BindsInstance
import dagger.Subcomponent

/**
 * Component used to add dependency injection about data into classes
 */
@UserScope
@Subcomponent(modules = [(UserModule::class)])
interface UserComponent {

    fun inject(playerService: PlayerService)
    fun inject(updateService: UpdateService)
    fun inject(pingService: PingService)
    fun inject(downloadStatusService: DownloadStatusService)

    fun playerComponentBuilder(): PlayerComponent.Builder

    @Subcomponent.Builder
    interface Builder {

        @BindsInstance
        fun ampacheApi(ampacheApi: AmpacheApi): Builder

        fun build(): UserComponent
    }
}