package be.florien.anyflow.tags.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Download")
data class DbDownload(
    @PrimaryKey
    val mediaId: Long,
    val mediaType: Int
)

data class DbDownloadedCount(
    val downloaded: Int,
    val count: Int
)

class DownloadProgressState(val total: Int, val downloaded: Int, val queued: Int)