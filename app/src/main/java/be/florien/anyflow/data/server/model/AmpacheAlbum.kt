package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Server-side data structures that relates to album
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheAlbum {
    var id: Long = 0
    var name: String = ""
    var artist: AmpacheArtistName = AmpacheArtistName()
    var year: Int = 0
    var tracks: Int = 0
    var disk: Int = 0
    var tag: List<AmpacheTagName> = mutableListOf()
    var art: String = ""
    var preciserating: Int = 0
    var rating: Double = 0.0
}

class AmpacheAlbumName {
    var id: Long = 0
    var name: String = ""
}