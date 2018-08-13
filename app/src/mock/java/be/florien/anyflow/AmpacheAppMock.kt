package be.florien.anyflow

import be.florien.anyflow.api.AmpacheApi
import be.florien.anyflow.api.MockUpAmpacheApi
import com.facebook.stetho.Stetho

/**
 * Application class used for initialization of many libraries
 */
class AnyFlowAppMock : AnyFlowApp() {

    override fun onCreate() {
        super.onCreate()
        Stetho.initializeWithDefaults(this)
    }

    override fun createUserScopeForServer(serverUrl: String): AmpacheApi {
        val ampacheApi = MockUpAmpacheApi(this)
        userComponent = applicationComponent
                .userComponentBuilder()
                .ampacheApi(ampacheApi)
                .build()
        return ampacheApi
    }
}