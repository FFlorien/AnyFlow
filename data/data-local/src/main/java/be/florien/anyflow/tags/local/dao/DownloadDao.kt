package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbDownload
import be.florien.anyflow.tags.local.model.DbSong
import be.florien.anyflow.tags.local.model.DownloadProgressState

@Dao
abstract class DownloadDao : BaseDao<DbDownload>() {
    @Query("SELECT * FROM download")
    abstract suspend fun allList(): List<DbDownload>

    @RawQuery
    abstract suspend fun rawQueryInsert(query: SupportSQLiteQuery): List<Long>

    @RawQuery(observedEntities = [DbDownload::class, DbSong::class])
    abstract fun rawQueryProgress(query: SupportSQLiteQuery): LiveData<DownloadProgressState>

}