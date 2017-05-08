package be.florien.ampacheplayer.model.realm

import be.florien.ampacheplayer.model.ampache.AmpacheTagName
import be.florien.ampacheplayer.model.ampache.AmpacheTag
import io.realm.RealmObject

/**
 * Database structure that represents to tags
 */
open class RealmTag : RealmObject {
    var id: Long = 0
    var name: String = ""

    constructor() : super()

    constructor(fromServer: AmpacheTag) : super() {
        id = fromServer.id
        name = fromServer.name
    }

    constructor(fromServer: AmpacheTagName) : super() {
        id = fromServer.id
        name = fromServer.value
    }
}