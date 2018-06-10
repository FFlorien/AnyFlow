package be.florien.ampacheplayer.persistence.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import be.florien.ampacheplayer.api.model.AmpacheAlbum
import io.realm.annotations.Index

/**
 * Database structure that represents to album
 */
@Entity
open class Album {
    @field:PrimaryKey
    var id: Long = 0
    var name: String = ""
    var artistName: String = ""
    @field:Index
    var artistId: Long = -1
    var year: Int = 0
    var tracks: Int = 0
    var disk: Int = 0
    @Ignore
    var tag: MutableList<Tag> = mutableListOf()
    var art: String = ""
    var preciserating: Int = 0
    var rating: Double = 0.0

    constructor() : super()

    constructor(fromServer: AmpacheAlbum) : super() {
        id = fromServer.id
        name = fromServer.name
        artistName = fromServer.artist.name
        artistId = fromServer.artist.id
        year = fromServer.year
        tracks = fromServer.tracks
        disk = fromServer.disk
        tag.addAll(fromServer.tag.map(::Tag))
        art = fromServer.art
        preciserating = fromServer.preciserating
        rating = fromServer.rating
    }
}