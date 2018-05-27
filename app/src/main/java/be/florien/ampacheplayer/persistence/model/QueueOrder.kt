package be.florien.ampacheplayer.persistence.model

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass

@RealmClass
open class QueueOrder() : RealmObject() {
    @field:PrimaryKey
    var order = 0
    var position = 0

    constructor(order: Int, position: Int) : this() {
        this.order = order
        this.position = position
    }
}