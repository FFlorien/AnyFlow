package be.florien.anyflow.data.server.model

/**
 * Server-side data structures that relates to playlist
 */

class AmpachePlayList {
    var id: Long = 0
    var name: String = ""
    var owner: String = ""
    var items: Int = 0
    var tag: List<AmpacheTagName> = mutableListOf()
}


class AmpachePlayListList {
    var total_count: Int = 0
    var playlists: List<AmpachePlayList> = mutableListOf()
    var error: AmpacheError = AmpacheError()
}