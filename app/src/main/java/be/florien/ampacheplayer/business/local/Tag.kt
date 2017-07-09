package be.florien.ampacheplayer.business.local

import be.florien.ampacheplayer.business.realm.RealmTag
import be.florien.ampacheplayer.business.ampache.AmpacheTagName
import be.florien.ampacheplayer.business.ampache.AmpacheTag

/**
 * Database structure that represents to tags
 */
data class Tag(var id: Long = 0,
               var name: String = "") {
    constructor(fromServer: AmpacheTag) : this(fromServer.id, fromServer.name)
    constructor(fromServer: AmpacheTagName) : this(fromServer.id, fromServer.value)
    constructor(fromRealm: RealmTag) : this(fromRealm.id, fromRealm.name)
}