package be.florien.anyflow.player

import be.florien.anyflow.persistence.local.model.DbOrder

class Order(private val priority: Int, val subject: Subject, val ordering: Ordering) {
    fun toDbOrder() = DbOrder(priority, subject.name, ordering.name)

    companion object {
        fun toOrder(order: DbOrder) = Order(order.priority, Subject.valueOf(order.subject), Ordering.valueOf(order.ordering))
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