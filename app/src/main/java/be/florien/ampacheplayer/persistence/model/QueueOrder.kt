package be.florien.ampacheplayer.persistence.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class QueueOrder() : RealmObject() {
    @field:PrimaryKey
    var order = 0
    var song: Song? = null

    constructor(order: Int, song: Song) : this() {
        this.order = order
        this.song = song
    }
}