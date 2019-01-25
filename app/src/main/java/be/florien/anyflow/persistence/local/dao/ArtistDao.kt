package be.florien.anyflow.persistence.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.persistence.local.model.Artist
import be.florien.anyflow.persistence.local.model.ArtistDisplay

@Dao
interface ArtistDao : BaseDao<Artist> {
    @Query("SELECT * FROM artist ORDER BY name COLLATE UNICODE")
    fun orderByName(): DataSource.Factory<Int, ArtistDisplay>
}