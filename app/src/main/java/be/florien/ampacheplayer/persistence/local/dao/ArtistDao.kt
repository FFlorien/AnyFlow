package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Artist
import io.reactivex.Flowable

@Dao
interface ArtistDao : BaseDao<Artist> {
    @Query("SELECT * FROM artist ORDER BY name")
    fun orderedByName(): Flowable<List<Artist>>
}