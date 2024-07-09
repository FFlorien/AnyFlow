package be.florien.anyflow.tags.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.tags.local.model.DbAlbum
import be.florien.anyflow.tags.local.model.DbAlbumDisplay
import be.florien.anyflow.tags.local.model.DbArtist

@Dao
abstract class AlbumDao : BaseDao<DbAlbum>() {
    @RawQuery(observedEntities = [DbAlbum::class, DbArtist::class])
    abstract suspend fun rawQueryDisplayList(query: SupportSQLiteQuery): List<DbAlbumDisplay>

    @RawQuery(observedEntities = [DbAlbum::class, DbArtist::class])
    abstract fun rawQueryDisplayPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbAlbumDisplay>
}