package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import be.florien.ampacheplayer.persistence.local.model.DbFilter
import io.reactivex.Flowable

@Dao
interface FilterDao : BaseDao<DbFilter> {
    @Query("SELECT * FROM dbfilter")
    fun all(): Flowable<List<DbFilter>>

    @Query("DELETE FROM dbfilter")
    fun deleteAll()
}