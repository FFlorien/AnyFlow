package be.florien.anyflow

import be.florien.anyflow.data.server.AmpacheApi
import be.florien.anyflow.data.user.UserComponent

interface UserComponentContainer {
    var userComponent: UserComponent?

    fun createUserScopeForServer(serverUrl: String): AmpacheApi
}