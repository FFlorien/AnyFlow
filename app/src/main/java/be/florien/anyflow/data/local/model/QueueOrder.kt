package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(foreignKeys = [ForeignKey(
        entity = Song::class,
        parentColumns = ["id"],
        childColumns = ["songId"],
        onDelete = ForeignKey.CASCADE)])
open class QueueOrder(
        @field:PrimaryKey
        val order: Int,
        val songId: Long)