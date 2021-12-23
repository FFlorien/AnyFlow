package be.florien.anyflow.data.view

sealed class Order(val priority: Int, val subject: Long, val ordering: Int, val argument: Int = -1) {
    class Random(priority: Int, subject: Long, randomSeed: Int) : Order(priority, subject, RANDOM, randomSeed)
    class Ordered(priority: Int, subject: Long) : Order(priority, subject, ASCENDING)
    class Precise(precisePosition: Int, songId: Long, priority: Int) : Order(priority, songId, PRECISE_POSITION, precisePosition)

    val orderingType
        get() = when (ordering) {
            ASCENDING -> Ordering.ASCENDING
            DESCENDING -> Ordering.DESCENDING
            PRECISE_POSITION -> Ordering.PRECISE_POSITION
            RANDOM -> Ordering.RANDOM
            else -> Ordering.RANDOM
        }

    val orderingSubject
        get() = when (subject) {
            SUBJECT_ALL -> Subject.ALL
            SUBJECT_ARTIST -> Subject.ARTIST
            SUBJECT_ALBUM_ARTIST -> Subject.ALBUM_ARTIST
            SUBJECT_ALBUM -> Subject.ALBUM
            SUBJECT_ALBUM_ID -> Subject.ALBUM_ID
            SUBJECT_YEAR -> Subject.YEAR
            SUBJECT_GENRE -> Subject.GENRE
            SUBJECT_TRACK -> Subject.TRACK
            SUBJECT_TITLE -> Subject.TITLE
            else -> Subject.TRACK
        }

    override fun equals(other: Any?) = other is Order && priority == other.priority && subject == other.subject && ordering == other.ordering && argument == other.argument

    companion object {

        const val PRIORITY_PRECISE = 2000
        const val ASCENDING = 1
        const val DESCENDING = -1
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
    }

    enum class Subject {
        ALL,
        ARTIST,
        ALBUM_ARTIST,
        ALBUM,
        ALBUM_ID,
        YEAR,
        GENRE,
        TRACK,
        TITLE
    }

    enum class Ordering {
        ASCENDING,
        DESCENDING,
        PRECISE_POSITION,
        RANDOM
    }

}