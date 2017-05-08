package be.florien.ampacheplayer.model.local

import be.florien.ampacheplayer.model.realm.RealmAlbum
import be.florien.ampacheplayer.model.ampache.AmpacheAlbum

/**
 * Database structure that represents to album
 */
data class Album(
        var id: Long = 0,
        var name: String = "",
        var artistName: String = "",
        var artistId: Long = -1,
        var year: Int = 0,
        var tracks: Int = 0,
        var disk: Int = 0,
        var tag: List<Tag> = emptyList<Tag>(),
        var art: String = "",
        var preciserating: Int = 0,
        var rating: Double = 0.0) {

    constructor(fromServer: AmpacheAlbum) : this(
            id = fromServer.id,
            name = fromServer.name,
            artistName = fromServer.artist.name,
            artistId = fromServer.artist.id,
            year = fromServer.year,
            tracks = fromServer.tracks,
            disk = fromServer.disk,
            tag = fromServer.tag.map(::Tag),
            art = fromServer.art,
            preciserating = fromServer.preciserating,
            rating = fromServer.rating)

    constructor(fromRealm: RealmAlbum) : this(
            id = fromRealm.id,
            name = fromRealm.name,
            artistName = fromRealm.artistName,
            artistId = fromRealm.artistId,
            year = fromRealm.year,
            tracks = fromRealm.tracks,
            disk = fromRealm.disk,
            tag = fromRealm.tag.map(::Tag),
            art = fromRealm.art,
            preciserating = fromRealm.preciserating,
            rating = fromRealm.rating)
}