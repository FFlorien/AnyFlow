package be.florien.ampacheplayer.model.local

import be.florien.ampacheplayer.model.realm.RealmTag
import be.florien.ampacheplayer.model.ampache.AmpacheTagName
import be.florien.ampacheplayer.model.ampache.AmpacheTag
import io.realm.RealmObject

/**
 * Database structure that represents to tags
 */
data class Tag(var id: Long = 0,
               var name: String = "") {
    constructor(fromServer: AmpacheTag) : this(fromServer.id, fromServer.name)
    constructor(fromServer: AmpacheTagName) : this(fromServer.id, fromServer.value)
    constructor(fromRealm: RealmTag) : this(fromRealm.id, fromRealm.name)
}