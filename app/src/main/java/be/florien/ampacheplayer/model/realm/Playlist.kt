package be.florien.ampacheplayer.model.realm

import be.florien.ampacheplayer.model.server.PlaylistServer
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Data structures that relates to playlist
 */

open class Playlist : RealmObject {
    @PrimaryKey
    var id: Long = 0
    var name: String = ""
    var owner: String = ""
    var tag: RealmList<Tag> = RealmList()

    constructor() : super()

    constructor(fromServer: PlaylistServer) : super(){
        id = fromServer.id
        name = fromServer.name
        owner = fromServer.owner
        tag.addAll(fromServer.tag.map(::Tag))
    }
}