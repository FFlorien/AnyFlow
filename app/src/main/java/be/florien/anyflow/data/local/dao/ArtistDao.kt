package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbArtist

@Dao
abstract class ArtistDao : BaseDao<DbArtist>() {
    @Query("SELECT * FROM artist ORDER BY name COLLATE UNICODE")
    abstract fun orderByName(): DataSource.Factory<Int, DbArtist>

    @Query("SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist INNER JOIN album ON album.artistId = artist.id ORDER BY artist.name COLLATE UNICODE")
    abstract fun albumArtistOrderByName(): DataSource.Factory<Int, DbArtist>

    @Query("SELECT * FROM artist WHERE name LIKE :filter ORDER BY name COLLATE UNICODE")
    abstract fun orderByNameFiltered(filter: String): DataSource.Factory<Int, DbArtist>

    @Query("SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist INNER JOIN album ON album.artistId = artist.id WHERE artist.name LIKE :filter ORDER BY artist.name COLLATE UNICODE")
    abstract fun albumArtistOrderByNameFiltered(filter: String): DataSource.Factory<Int, DbArtist>

    @Query("SELECT * FROM artist WHERE name LIKE :filter ORDER BY name COLLATE UNICODE")
    abstract suspend fun orderByNameFilteredList(filter: String): List<DbArtist>

    @Query("SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist INNER JOIN album ON album.artistId = artist.id WHERE artist.name LIKE :filter ORDER BY artist.name COLLATE UNICODE")
    abstract suspend fun albumArtistOrderByNameFilteredList(filter: String): List<DbArtist>
}