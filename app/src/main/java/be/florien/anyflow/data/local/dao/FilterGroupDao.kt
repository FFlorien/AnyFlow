package be.florien.anyflow.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbFilterGroup
import io.reactivex.Flowable

@Dao
abstract class FilterGroupDao : BaseDao<DbFilterGroup> {
    @Query("SELECT * FROM filtergroup")
    abstract fun all(): Flowable<List<DbFilterGroup>>

    @Query("SELECT * FROM filtergroup WHERE id != 1")
    abstract fun allSavedFilterGroup(): Flowable<List<DbFilterGroup>>

    @Query("DELETE FROM filtergroup WHERE id = :id")
    abstract fun deleteGroup(id: Int)

    @Query("DELETE FROM filtergroup")
    abstract fun deleteAll()
}