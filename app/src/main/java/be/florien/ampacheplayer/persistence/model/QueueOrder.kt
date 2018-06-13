package be.florien.ampacheplayer.persistence.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.ForeignKey
import android.arch.persistence.room.PrimaryKey

@Entity(foreignKeys = [ForeignKey(
        entity = Song::class,
        parentColumns = ["id"],
        childColumns = ["songId"],
        onDelete = ForeignKey.CASCADE)])
open class QueueOrder() {
    @field:PrimaryKey
    var order = 0
    var songId:Long = 0

    constructor(order: Int, song: Song) : this() {
        this.order = order
        this.songId = song.id
    }
}