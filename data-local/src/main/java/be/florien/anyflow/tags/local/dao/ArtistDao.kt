package be.florien.anyflow.tags.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbArtist

@Dao
abstract class ArtistDao : BaseDao<DbArtist>() {
    @RawQuery(observedEntities = [DbArtist::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbArtist>
}