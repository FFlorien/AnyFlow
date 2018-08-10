package be.florien.anyflow.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.anyflow.persistence.local.model.Artist
import be.florien.anyflow.persistence.local.model.ArtistDisplay
import io.reactivex.Flowable

@Dao
interface ArtistDao : BaseDao<Artist> {
    @Query("SELECT * FROM artist ORDER BY name COLLATE UNICODE")
    fun orderByName(): Flowable<List<ArtistDisplay>>
}