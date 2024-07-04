package be.florien.anyflow.data.local.query

sealed class QueryOrdering(val priority: Int, val subject: Subject, val argument: Int = -1) {
    class Random(priority: Int, subject: Subject, randomSeed: Int) : QueryOrdering(priority, subject, randomSeed)
    class Ordered(priority: Int, subject: Subject) : QueryOrdering(priority, subject)
    class Precise(priority: Int, subject: Subject, precisePosition: Int, val songId: Long) : QueryOrdering(priority, subject, precisePosition)

    enum class Subject(val clause: String) {
        ALL(" song.id"),
        ARTIST(" artist.basename"),
        ALBUM_ARTIST(" albumArtist.basename"),
        ALBUM(" album.basename"),
        ALBUM_ID(" song.albumId"),
        DISC(" song.disk"),
        YEAR(" song.year"),
        GENRE(" song.genre"),
        TRACK(" song.track"),
        TITLE(" song.titleForSort"),
    }

}