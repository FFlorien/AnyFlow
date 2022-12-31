package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbArtist

@Dao
abstract class ArtistDao : BaseDao<DbArtist>() {
    @Query("SELECT * FROM artist ORDER BY name COLLATE UNICODE")
    abstract fun orderByName(): DataSource.Factory<Int, DbArtist>

    @Query("SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist INNER JOIN album ON album.artistId = artist.id ORDER BY artist.name COLLATE UNICODE")
    abstract fun albumArtistOrderByName(): DataSource.Factory<Int, DbArtist>

    @Query("SELECT * FROM artist WHERE name LIKE :search ORDER BY name COLLATE UNICODE")
    abstract fun orderByNameSearched(search: String): DataSource.Factory<Int, DbArtist>

    @Query("SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist INNER JOIN album ON album.artistId = artist.id WHERE artist.name LIKE :search ORDER BY artist.name COLLATE UNICODE")
    abstract fun albumArtistOrderByNameSearched(search: String): DataSource.Factory<Int, DbArtist>

    @Query("SELECT * FROM artist WHERE name LIKE :search ORDER BY name COLLATE UNICODE")
    abstract suspend fun orderByNameSearchedList(search: String): List<DbArtist>

    @Query("SELECT DISTINCT artist.id, artist.name, artist.summary FROM artist INNER JOIN album ON album.artistId = artist.id WHERE artist.name LIKE :search ORDER BY artist.name COLLATE UNICODE")
    abstract suspend fun albumArtistOrderByNameSearchedList(search: String): List<DbArtist>

    @RawQuery(observedEntities = [DbArtist::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbArtist>
}