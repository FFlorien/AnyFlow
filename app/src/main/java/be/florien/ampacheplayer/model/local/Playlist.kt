package be.florien.ampacheplayer.model.local

import be.florien.ampacheplayer.model.realm.RealmPlaylist
import be.florien.ampacheplayer.model.ampache.AmpachePlayList
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Database structure that represents to playlist
 */

data class Playlist(
        var id: Long = 0,
        var name: String = "",
        var owner: String = "",
        var tag: List<Tag> = emptyList<Tag>()) {

    constructor(fromServer: AmpachePlayList) : this(
            id = fromServer.id,
            name = fromServer.name,
            owner = fromServer.owner,
            tag = fromServer.tag.map(::Tag))

    constructor(fromRealm: RealmPlaylist) : this(
            id = fromRealm.id,
            name = fromRealm.name,
            owner = fromRealm.owner,
            tag = fromRealm.tag.map(::Tag))
}