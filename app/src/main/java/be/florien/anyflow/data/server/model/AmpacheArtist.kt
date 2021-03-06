package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Server-side data structures that relates to artists
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheArtist {
    var id: Long = 0
    var name: String = ""
    var albums: String = ""
    var art: String = ""
    var songs: String = ""
    var tag: List<AmpacheTagName> = mutableListOf()
    var preciserating: Int = 0
    var rating: Double = 0.0

}

class AmpacheAlbumArtist {
    var id: Long = 0
    var name: String = ""
}

class AmpacheArtistName {
    var id: Long = 0
    var name: String = ""
}
