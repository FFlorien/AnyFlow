package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbDownload
import be.florien.anyflow.data.local.model.DbSong
import be.florien.anyflow.data.local.model.DownloadProgressState

@Dao
abstract class DownloadDao : BaseDao<DbDownload>() {
    @Query("SELECT * FROM download")
    abstract suspend fun allList(): List<DbDownload>

    @RawQuery
    abstract suspend fun rawQueryInsert(query: SupportSQLiteQuery): List<Long>

    @RawQuery(observedEntities = [DbDownload::class, DbSong::class])
    abstract fun rawQueryProgress(query: SupportSQLiteQuery): LiveData<DownloadProgressState>

}