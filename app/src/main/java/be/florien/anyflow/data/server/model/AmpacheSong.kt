package be.florien.anyflow.data.server.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * Server-side data structures that relates to accounts
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
class AmpacheSong {
    var id: Long = 0
    var song: String = ""
    var title: String = ""
    var name: String = ""
    var artist: AmpacheArtistName = AmpacheArtistName()
    var album: AmpacheAlbumName = AmpacheAlbumName()
    var albumartist: AmpacheAlbumArtist = AmpacheAlbumArtist()
    var tag: List<AmpacheTagName> = mutableListOf()
    var filename: String = ""
    var track: Int = 0
    var time: Int = 0
    var year: Int = 0
    var bitrate: Int = 0
    var rate: Int = 0
    var mode: String = ""
    var mime: String = ""
    var url: String = ""
    var size: Int = 0
    var art: String = ""
    var preciserating: Int = 0
    var rating: Int = 0
    var averagerating: Double = 0.0
    var composer: String = ""
    var replaygain_album_gain: Double = 0.0
    var replaygain_album_peak: Double = 0.0
    var replaygain_track_gain: Double = 0.0
    var replaygain_track_peak: Double = 0.0
    var genre: List<AmpacheGenre> = mutableListOf()
}

class AmpacheGenre {
    var name: String = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
class AmpacheSongId {
    var id: Long = 0
}
