package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbAlbum
import be.florien.anyflow.data.local.model.DbAlbumDisplay

@Dao
interface AlbumDao : BaseDao<DbAlbum> {
    @Query("SELECT * FROM album ORDER BY name COLLATE UNICODE")
    fun orderByName(): DataSource.Factory<Int, DbAlbumDisplay>

    @Query("UPDATE Album SET artistId = :id, artistName = :name WHERE id = :albumId")
    fun updateAlbumArtist(albumId: Long, id: Long, name: String)
}