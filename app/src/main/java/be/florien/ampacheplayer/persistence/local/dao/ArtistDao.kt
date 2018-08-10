package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Artist
import be.florien.ampacheplayer.persistence.local.model.ArtistDisplay
import io.reactivex.Flowable

@Dao
interface ArtistDao : BaseDao<Artist> {
    @Query("SELECT * FROM artist ORDER BY name COLLATE UNICODE")
    fun orderByName(): Flowable<List<ArtistDisplay>>
}