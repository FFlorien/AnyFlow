package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbAlbum
import be.florien.anyflow.data.local.model.DbAlbumDisplay

@Dao
abstract class AlbumDao : BaseDao<DbAlbum>() {
    @Transaction
    @Query("SELECT * FROM album ORDER BY album.name COLLATE UNICODE")
    abstract fun orderByName(): DataSource.Factory<Int, DbAlbumDisplay>

    @Transaction
    @Query("SELECT * FROM album WHERE album.name LIKE :search ORDER BY album.name COLLATE UNICODE")
    abstract fun orderByNameSearched(search: String): DataSource.Factory<Int, DbAlbumDisplay>

    @Transaction
    @Query("SELECT * FROM album WHERE album.name LIKE :search ORDER BY album.name COLLATE UNICODE")
    abstract suspend fun orderByNameSearchedList(search: String): List<DbAlbumDisplay>

    @RawQuery(observedEntities = [DbAlbumDisplay::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbAlbumDisplay>
}