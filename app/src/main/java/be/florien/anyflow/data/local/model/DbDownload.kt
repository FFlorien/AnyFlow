package be.florien.anyflow.data.local.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Download")
data class DbDownload constructor(
    @PrimaryKey
    val songId: Long
)

class DownloadProgressState(val total: Int, val downloaded: Int, val queued: Int)