package be.florien.anyflow.injection

import be.florien.anyflow.UserComponentContainer
import be.florien.anyflow.data.PingService
import be.florien.anyflow.data.UpdateService
import be.florien.anyflow.data.server.AmpacheApi
import be.florien.anyflow.data.user.UserComponent
import be.florien.anyflow.feature.player.PlayerActivity
import be.florien.anyflow.feature.player.PlayerComponent
import be.florien.anyflow.player.PlayerService
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class UserComponentContainerFake : UserComponentContainer {
    override var userComponent: UserComponent?
        get() = UserComponentStub()
        set(value) {}

    override fun createUserScopeForServer(serverUrl: String): AmpacheApi {
        val mockWebServer = MockWebServer()
        val dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (!serverUrl.contains("ampache")) {
                    return MockResponse().setResponseCode(404)
                }
                return when (request.requestUrl?.queryParameter("action")) {
                    "handshake" -> {
                        when (request.requestUrl?.queryParameter("user")) {
                            "admin" -> {
                                MockResponse().setResponseCode(200).setBody(readFromFile("handshake_success.xml"))
                            }
                            else -> {
                                MockResponse().setResponseCode(200).setBody(readFromFile("handshake_failure.xml"))
                            }
                        }
                    }
                    "ping" -> MockResponse().setResponseCode(200).setBody(readFromFile("ping.xml"))
                    "songs" -> MockResponse().setResponseCode(200).setBody(readFromFile("handshake"))
                    "artists" -> MockResponse().setResponseCode(200).setBody(readFromFile("handshake"))
                    "albums" -> MockResponse().setResponseCode(200).setBody(readFromFile("handshake"))
                    "tags" -> MockResponse().setResponseCode(200).setBody(readFromFile("handshake"))
                    "playlists" -> MockResponse().setResponseCode(200).setBody(readFromFile("handshake"))
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        mockWebServer.dispatcher = dispatcher
        return Retrofit.Builder()
                .baseUrl(mockWebServer.url("/"))
                .addConverterFactory(SimpleXmlConverterFactory.create())
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

    fun readFromFile(filename: String): String {
        val classLoader = this.javaClass.classLoader
        val resource = classLoader?.getResourceAsStream(filename)
        val reader = resource?.reader()
        val value = reader?.readText() ?: ""
        reader?.close()
        return value
    }
}