package be.florien.anyflow.tags.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbGenre

@Dao
abstract class GenreDao : BaseDao<DbGenre>() {
    @RawQuery(observedEntities = [DbGenre::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbGenre>
}