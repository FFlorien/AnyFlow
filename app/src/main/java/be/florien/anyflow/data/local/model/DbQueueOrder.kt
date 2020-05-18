package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "QueueOrder",
        foreignKeys = [ForeignKey(
                entity = DbSong::class,
                parentColumns = ["id"],
                childColumns = ["songId"],
                onDelete = ForeignKey.CASCADE)])
data class DbQueueOrder(
        @field:PrimaryKey
        val order: Int,
        val songId: Long)