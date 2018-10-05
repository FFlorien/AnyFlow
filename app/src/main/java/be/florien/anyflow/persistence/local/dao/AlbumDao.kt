package be.florien.anyflow.persistence.local.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import be.florien.anyflow.persistence.local.model.Album
import be.florien.anyflow.persistence.local.model.AlbumDisplay
import io.reactivex.Flowable

@Dao
interface AlbumDao : BaseDao<Album> {
    @Query("SELECT * FROM album ORDER BY name COLLATE UNICODE")
    fun orderByName(): Flowable<List<AlbumDisplay>>
}