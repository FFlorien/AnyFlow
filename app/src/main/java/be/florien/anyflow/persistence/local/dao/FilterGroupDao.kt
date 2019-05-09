package be.florien.anyflow.persistence.local.dao

import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.persistence.local.model.FilterGroup
import io.reactivex.Flowable

@Dao
abstract class FilterGroupDao : BaseDao<FilterGroup> {
    @Query("SELECT * FROM filtergroup")
    abstract fun all(): Flowable<List<FilterGroup>>

    @Query("SELECT * FROM filtergroup WHERE id != 1")
    abstract fun allSavedFilterGroup(): Flowable<List<FilterGroup>>

    @Query("DELETE FROM filtergroup WHERE id = :id")
    abstract fun deleteGroup(id: Int)

    @Query("DELETE FROM filtergroup")
    abstract fun deleteAll()
}