package be.florien.anyflow.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "QueueOrder")// todo foreign key?
//        foreignKeys = [ForeignKey(
//                entity = DbSong::class,
//                parentColumns = ["id"],
//                childColumns = ["id"],
//                onDelete = ForeignKey.CASCADE)])
data class DbQueueOrder(
        @field:PrimaryKey
        val order: Int,
        @ColumnInfo(index = true)
        val id: Long,
        val mediaType: Int)

data class DbQueueItem(
        val id: Long,
        val mediaType: Int
)

data class DbMediaToPlay(
        val id: Long,
        val local: String?,
        val mediaType: Int
)

const val SONG_MEDIA_TYPE = 0
const val PODCAST_MEDIA_TYPE = 1