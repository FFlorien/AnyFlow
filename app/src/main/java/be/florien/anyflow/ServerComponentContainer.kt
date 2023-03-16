package be.florien.anyflow

import be.florien.anyflow.injection.ServerComponent

interface ServerComponentContainer {
    var serverComponent: ServerComponent?

    fun createUserScopeForServer(serverUrl: String)
}