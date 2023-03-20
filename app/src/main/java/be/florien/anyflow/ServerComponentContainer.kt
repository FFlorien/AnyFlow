package be.florien.anyflow

import be.florien.anyflow.injection.ServerComponent

interface ServerComponentContainer {
    var serverComponent: ServerComponent?

    suspend fun createServerComponentIfServerValid(serverUrl: String): Boolean
}