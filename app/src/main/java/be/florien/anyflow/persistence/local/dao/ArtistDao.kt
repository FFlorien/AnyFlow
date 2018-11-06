package be.florien.anyflow.persistence.local.dao

import android.arch.paging.DataSource
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import be.florien.anyflow.persistence.local.model.Artist
import be.florien.anyflow.persistence.local.model.ArtistDisplay

@Dao
interface ArtistDao : BaseDao<Artist> {
    @Query("SELECT * FROM artist ORDER BY name COLLATE UNICODE")
    fun orderByName(): DataSource.Factory<Int, ArtistDisplay>
}