package be.florien.anyflow.tags.local.query

sealed class QueryOrdering(val priority: Int, val subject: Subject, val argument: Int = -1) {
    class Random(priority: Int, subject: Subject, randomSeed: Int) : QueryOrdering(priority, subject, randomSeed)
    class Ordered(priority: Int, subject: Subject) : QueryOrdering(priority, subject) {
        override fun getOrderingClause(): String = "${subject.clause} ASC"
    }
    class Precise(priority: Int, subject: Subject, precisePosition: Int, val songId: Long) : QueryOrdering(priority, subject, precisePosition)

    open fun getOrderingClause(): String = subject.clause

    fun getJoin() = subject.joinType?.let { QueryJoin(it, 0) }

    enum class Subject(val clause: String, val joinType: QueryJoin.JoinType? = null) {
        ALL("song.id"),
        ARTIST("artist.basename", QueryJoin.JoinType.ARTIST),
        ALBUM_ARTIST("albumArtist.basename", QueryJoin.JoinType.ALBUM_ARTIST),
        ALBUM("album.basename", QueryJoin.JoinType.ALBUM),
        ALBUM_ID("song.albumId"),
        DISC("song.disk"),
        YEAR("song.year"),
        GENRE("song.genre", QueryJoin.JoinType.GENRE),
        TRACK("song.track"),
        TITLE("song.titleForSort"),
    }

}