package be.florien.ampacheplayer.business.realm

import be.florien.ampacheplayer.business.ampache.AmpacheTagName
import be.florien.ampacheplayer.business.ampache.AmpacheTag
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Database structure that represents to tags
 */
open class Tag : RealmObject {
    @field:PrimaryKey
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