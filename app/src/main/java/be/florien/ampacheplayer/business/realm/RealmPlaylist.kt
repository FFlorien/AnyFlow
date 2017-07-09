package be.florien.ampacheplayer.business.realm

import be.florien.ampacheplayer.business.ampache.AmpachePlayList
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Database structure that represents to playlist
 */

open class RealmPlaylist : RealmObject {
    @PrimaryKey
    var id: Long = 0
    var name: String = ""
    var owner: String = ""
    var tag: RealmList<RealmTag> = RealmList()

    constructor() : super()

    constructor(fromServer: AmpachePlayList) : super(){
        id = fromServer.id
        name = fromServer.name
        owner = fromServer.owner
        tag.addAll(fromServer.tag.map(::RealmTag))
    }
}