package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.data.local.model.DbAlbum
import be.florien.anyflow.data.local.model.DbAlbumDisplay

@Dao
abstract class AlbumDao : BaseDao<DbAlbum>() {
    @Transaction
    @Query("SELECT * FROM album ORDER BY album.name COLLATE UNICODE")
    abstract fun orderByName(): DataSource.Factory<Int, DbAlbumDisplay>

    @Transaction
    @Query("SELECT * FROM album WHERE album.name LIKE :filter ORDER BY album.name COLLATE UNICODE")
    abstract fun orderByNameFiltered(filter: String): DataSource.Factory<Int, DbAlbumDisplay>

    @Transaction
    @Query("SELECT * FROM album WHERE album.name LIKE :filter ORDER BY album.name COLLATE UNICODE")
    abstract suspend fun orderByNameFilteredList(filter: String): List<DbAlbumDisplay>
}