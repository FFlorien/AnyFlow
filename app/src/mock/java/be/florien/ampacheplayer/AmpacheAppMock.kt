package be.florien.ampacheplayer

import be.florien.ampacheplayer.api.AmpacheApi
import be.florien.ampacheplayer.api.MockUpAmpacheApi
import com.facebook.stetho.Stetho

/**
 * Application class used for initialization of many libraries
 */
class AmpacheAppMock : AmpacheApp() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }

    override fun createUserScopeForServer(serverUrl: String): AmpacheApi {
        val ampacheApi = MockUpAmpacheApi()
        userComponent = applicationComponent
                .userComponentBuilder()
                .ampacheApi(ampacheApi)
                .build()
        return ampacheApi
    }
}