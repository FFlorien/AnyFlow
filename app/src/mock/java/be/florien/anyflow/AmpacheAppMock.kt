package be.florien.anyflow

import be.florien.anyflow.api.AmpacheApi
import be.florien.anyflow.api.MockUpAmpacheApi

/**
 * Application class used for initialization of many libraries
 */
class AnyFlowAppMock : AnyFlowApp() {

    override fun createUserScopeForServer(serverUrl: String): AmpacheApi {
        val ampacheApi = MockUpAmpacheApi(this)
        userComponent = applicationComponent
                .userComponentBuilder()
                .ampacheApi(ampacheApi)
                .build()
        return ampacheApi
    }
}