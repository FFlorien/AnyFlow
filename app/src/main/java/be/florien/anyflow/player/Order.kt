package be.florien.anyflow.player

import be.florien.anyflow.persistence.local.model.DbOrder

class Order(private val priority: Int, val subject: Subject, val ordering: Int) {
    constructor(priority: Int, subject: Subject) : this(priority, subject, Math.random().times(1000).plus(2).toInt())

    fun toDbOrder() = DbOrder(priority, subject.name, ordering)

    val isRandom
        get() = ordering != ASCENDING && ordering != DESCENDING

    companion object {
        fun toOrder(order: DbOrder) = Order(order.priority, Subject.valueOf(order.subject), order.ordering)
        const val ASCENDING = 1
        const val DESCENDING = -1
    }
}

enum class Subject {
    ALL,
    ARTIST,
    ALBUM_ARTIST,
    ALBUM,
    YEAR,
    GENRE,
    TRACK,
    TITLE
}

enum class Ordering {
    ASCENDING,
    DESCENDING,
    RANDOM
}