package be.florien.ampacheplayer.persistence.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

@Entity
open class QueueOrder() {
    @field:PrimaryKey
    var order = 0
    var position = 0

    constructor(order: Int, position: Int) : this() {
        this.order = order
        this.position = position
    }
}