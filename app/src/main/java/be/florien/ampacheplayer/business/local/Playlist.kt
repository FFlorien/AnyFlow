package be.florien.ampacheplayer.business.local

import be.florien.ampacheplayer.business.realm.RealmPlaylist
import be.florien.ampacheplayer.business.ampache.AmpachePlayList

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