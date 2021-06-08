package be.florien.anyflow.data.server.model

/**
 * Server-side data structures that relates to tags
 */
class AmpacheTag {
    var id: Long = 0
    var name: String = ""
    var albums: Int = 0
    var artists: Int = 0
    var songs: Int = 0
    var video: Int = 0
    var playlist: Int = 0
    var stream: Int = 0
}

class AmpacheTagName {
    var id: Long = 0
    var name: String = ""
}