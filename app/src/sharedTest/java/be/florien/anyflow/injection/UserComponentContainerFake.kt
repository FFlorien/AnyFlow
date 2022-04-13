package be.florien.anyflow.injection

import be.florien.anyflow.UserComponentContainer
import be.florien.anyflow.data.PingService
import be.florien.anyflow.data.UpdateService
import be.florien.anyflow.data.server.AmpacheApi
import be.florien.anyflow.data.server.AmpacheServerFakeDispatcher
import be.florien.anyflow.data.user.UserComponent
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.player.PlayerService
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

class UserComponentContainerFake : UserComponentContainer {
    override var userComponent: UserComponent?
        get() = UserComponentStub()
        set(value) {}

    override fun createUserScopeForServer(serverUrl: String): AmpacheApi {
        val mockWebServer = MockWebServer()
        val dispatcher = AmpacheServerFakeDispatcher(serverUrl)

        mockWebServer.dispatcher = dispatcher
        return Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(AmpacheApi::
                class.java)
    }

    class UserComponentStub : UserComponent {
        override fun inject(playerService: PlayerService) {
            // Stub !
        }

        override fun inject(updateService: UpdateService) {
            // Stub !
        }

        override fun inject(pingService: PingService) {
            // Stub !
        }

        override fun playerComponentBuilder(): PlayerComponent.Builder = PlayerComponentStub.Builder()
    }

    class PlayerComponentStub : PlayerComponent {
        override fun inject(playerActivity: PlayerActivity) {
            // Stub !
        }

        class Builder : PlayerComponent.Builder {
            override fun build() = PlayerComponentStub()
        }
    }
}