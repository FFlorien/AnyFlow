package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Tag
import io.reactivex.Flowable

@Dao
interface TagDao : BaseDao<Tag> {
    @Query("SELECT * FROM tag")
    fun all(): Flowable<List<Tag>>
}