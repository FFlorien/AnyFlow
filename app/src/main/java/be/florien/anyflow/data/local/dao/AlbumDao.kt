package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.Album
import be.florien.anyflow.data.local.model.AlbumDisplay

@Dao
interface AlbumDao : BaseDao<Album> {
    @Query("SELECT * FROM album ORDER BY name COLLATE UNICODE")
    fun orderByName(): DataSource.Factory<Int, AlbumDisplay>

    @Query("UPDATE Album SET artistId = :id, artistName = :name WHERE id = :albumId")
    fun updateAlbumArtist(albumId: Long, id: Long, name: String)
}