package be.florien.anyflow.data.view

sealed class Ordering(val priority: Int, val subject: Long, val ordering: Int, val argument: Int = -1) {
    class Random(priority: Int, subject: Long, randomSeed: Int) : Ordering(priority, subject, RANDOM, randomSeed)
    class Ordered(priority: Int, subject: Long) : Ordering(priority, subject, ASCENDING)
    class Precise(precisePosition: Int, songId: Long, priority: Int) : Ordering(priority, songId, PRECISE_POSITION, precisePosition)

    val orderingType
        get() = when (ordering) {
            ASCENDING -> OrderingType.ASCENDING
            PRECISE_POSITION -> OrderingType.PRECISE_POSITION
            RANDOM -> OrderingType.RANDOM
            else -> OrderingType.RANDOM
        }

    val orderingSubject
        get() = when (subject) {
            SUBJECT_ALL -> Subject.ALL
            SUBJECT_ARTIST -> Subject.ARTIST
            SUBJECT_ALBUM_ARTIST -> Subject.ALBUM_ARTIST
            SUBJECT_ALBUM -> Subject.ALBUM
            SUBJECT_ALBUM_ID -> Subject.ALBUM_ID
            SUBJECT_DISC -> Subject.DISC
            SUBJECT_YEAR -> Subject.YEAR
            SUBJECT_GENRE -> Subject.GENRE
            SUBJECT_TRACK -> Subject.TRACK
            SUBJECT_TITLE -> Subject.TITLE
            else -> Subject.TRACK
        }

    override fun equals(other: Any?) = other is Ordering && priority == other.priority && subject == other.subject && ordering == other.ordering && argument == other.argument

    override fun hashCode(): Int {
        var result = priority
        result = 31 * result + subject.hashCode()
        result = 31 * result + ordering
        result = 31 * result + argument
        return result
    }

    companion object {

        const val PRIORITY_PRECISE = 2000
        const val ASCENDING = 1
        const val PRECISE_POSITION = -2
        const val RANDOM = -3
        const val RANDOM_MULTIPLIER = 1000
        const val SUBJECT_ALL = -1L
        const val SUBJECT_ARTIST = -2L
        const val SUBJECT_ALBUM_ARTIST = -3L
        const val SUBJECT_ALBUM = -4L
        const val SUBJECT_YEAR = -5L
        const val SUBJECT_GENRE = -6L
        const val SUBJECT_TRACK = -7L
        const val SUBJECT_TITLE = -8L
        const val SUBJECT_ALBUM_ID = -9L
        const val SUBJECT_DISC = -10L
    }

    enum class Subject {
        ALL,
        ARTIST,
        ALBUM_ARTIST,
        ALBUM,
        ALBUM_ID,
        DISC,
        YEAR,
        GENRE,
        TRACK,
        TITLE
    }

    enum class OrderingType {
        ASCENDING,
        PRECISE_POSITION,
        RANDOM
    }

}