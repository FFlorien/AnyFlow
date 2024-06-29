package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbAlbum
import be.florien.anyflow.data.local.model.DbAlbumWithArtist
import be.florien.anyflow.data.local.model.DbAlbumDisplay

@Dao
abstract class AlbumDao : BaseDao<DbAlbum>() {
    @RawQuery(observedEntities = [DbAlbumWithArtist::class])
    abstract suspend fun rawQueryDisplayList(query: SupportSQLiteQuery): List<DbAlbumDisplay>

    @RawQuery(observedEntities = [DbAlbumWithArtist::class])
    abstract fun rawQueryDisplayPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbAlbumDisplay>
}