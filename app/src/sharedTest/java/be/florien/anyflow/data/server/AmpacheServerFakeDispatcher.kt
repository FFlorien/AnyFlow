package be.florien.anyflow.data.server

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import java.util.*

class AmpacheServerFakeDispatcher(private val serverUrl: String) : Dispatcher() {
    private var authConnected: String? = null
    private var authValidity: Date? = null

    private var authGenerator = "1"

    override fun dispatch(request: RecordedRequest): MockResponse {
        if (!serverUrl.contains(GOOD_URL)) {
            return MockResponse().setResponseCode(404)
        }
        return when (request.requestUrl?.queryParameter(QUERY_ACTION)) {
            ACTION_HANDSHAKE -> {
                when (request.requestUrl?.queryParameter(QUERY_USER)) {
                    USER_NAME -> {
                        val response = getResponse("handshake_success.xml") { it.replace(SCHEME_TOKEN, authGenerator) }
                        authConnected = authGenerator
                        authGenerator += authGenerator
                        response
                    }
                    else -> {
                        getResponse("handshake_failure.xml")
                    }
                }
            }
            ACTION_PING -> verifyAuthToken(request) { getResponse("ping.xml") }
            ACTION_SONGS -> verifyAuthToken(request) { getResponse("songs.xml") }
            ACTION_ARTISTS -> verifyAuthToken(request) { getResponse("artists.xml") }
            ACTION_ALBUMS -> verifyAuthToken(request) { getResponse("albums.xml") }
            ACTION_TAGS -> verifyAuthToken(request) { getResponse("tags.xml") }
            ACTION_PLAYLISTS -> verifyAuthToken(request) { getResponse("playlists.xml") }
            else -> MockResponse().setResponseCode(404)
        }
    }

    private inline fun getResponse(responseFileName: String, transformation: (String) -> String = { it }) =
            MockResponse().setResponseCode(200).setBody(transformation(readFromFile(responseFileName)))

    private fun readFromFile(filename: String): String {
        val classLoader = this.javaClass.classLoader
        val resource = classLoader?.getResourceAsStream(filename)
        val reader = resource?.reader()
        val value = reader?.readText() ?: ""
        reader?.close()
        return value
    }

    private inline fun verifyAuthToken(request: RecordedRequest, get200Response: () -> MockResponse): MockResponse {
        return if (request.requestUrl?.queryParameter(QUERY_AUTH) == authConnected && authValidity?.after(Date()) == true) { // todo verify difference between ping too late and ping not connected
            get200Response()
        } else {
            MockResponse().setResponseCode(501) // Todo send it back as it is on ampache server
        }
    }


    companion object {
        const val USER_NAME = "admin"
        const val PASSWORD = "password"
        const val GOOD_URL = "http://www.ampache.com/"
        const val QUERY_ACTION = "action"
        const val QUERY_USER = "user"
        const val QUERY_AUTH = "auth"
        const val SCHEME_TOKEN = "#authToken#"
        const val ACTION_HANDSHAKE = "handshake"
        const val ACTION_PING = "ping"
        const val ACTION_SONGS = "songs"
        const val ACTION_ARTISTS = "artists"
        const val ACTION_ALBUMS = "albums"
        const val ACTION_TAGS = "tags"
        const val ACTION_PLAYLISTS = "playlists"
    }
}