package be.florien.anyflow.persistence.local.dao

import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.persistence.local.model.DbFilter
import io.reactivex.Flowable

@Dao
abstract class FilterDao : BaseDao<DbFilter> {
    @Query("SELECT * FROM dbfilter WHERE filterGroup = 1")
    abstract fun currentFilters(): Flowable<List<DbFilter>>

    @Query("SELECT * FROM dbfilter WHERE filterGroup = :groupId")
    abstract fun filterForGroup(groupId: Long): List<DbFilter>

    @Query("DELETE FROM dbfilter WHERE filterGroup = 1")
    abstract fun deleteCurrentFilters()
}