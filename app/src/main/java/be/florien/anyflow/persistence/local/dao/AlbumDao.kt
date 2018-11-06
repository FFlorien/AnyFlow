package be.florien.anyflow.persistence.local.dao

import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import be.florien.anyflow.persistence.local.model.Album
import be.florien.anyflow.persistence.local.model.AlbumDisplay

@Dao
interface AlbumDao : BaseDao<Album> {
    @Query("SELECT * FROM album ORDER BY name COLLATE UNICODE")
    fun orderByName(): DataSource.Factory<Int, AlbumDisplay>
}