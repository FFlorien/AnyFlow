package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbAlbum
import be.florien.anyflow.data.local.model.DbAlbumDisplay
import be.florien.anyflow.data.local.model.DbAlbumDisplayForRaw

@Dao
abstract class AlbumDao : BaseDao<DbAlbum>() {
    @RawQuery(observedEntities = [DbAlbumDisplay::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbAlbumDisplayForRaw>

    @RawQuery(observedEntities = [DbAlbumDisplay::class])
    abstract suspend fun rawQueryListDisplay(query: SupportSQLiteQuery): List<DbAlbumDisplayForRaw>
}