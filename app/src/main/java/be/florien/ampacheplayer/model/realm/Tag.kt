package be.florien.ampacheplayer.model.realm

import be.florien.ampacheplayer.model.server.TagName
import be.florien.ampacheplayer.model.server.TagServer
import io.realm.RealmObject

/**
 * Data structures that relates to tags
 */
open class Tag : RealmObject {
    var id: Long = 0
    var name: String = ""

    constructor() : super()

    constructor(fromServer: TagServer) : super() {
        id = fromServer.id
        name = fromServer.name
    }

    constructor(fromServer: TagName) : super() {
        id = fromServer.id
        name = fromServer.value
    }
}