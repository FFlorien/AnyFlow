package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbGenre

@Dao
abstract class GenreDao : BaseDao<DbGenre>() {
    @Query("SELECT * FROM genre ORDER BY name COLLATE UNICODE")
    abstract fun genreOrderByGenre(): DataSource.Factory<Int, DbGenre>

    @Query("SELECT * FROM genre WHERE name LIKE :filter ORDER BY name COLLATE UNICODE")
    abstract fun genreOrderByGenreSearched(filter: String): DataSource.Factory<Int, DbGenre>

    @Query("SELECT * FROM genre WHERE name LIKE :filter ORDER BY name COLLATE UNICODE")
    abstract suspend fun genreOrderByGenreSearchedList(filter: String): List<DbGenre>

    @RawQuery(observedEntities = [DbGenre::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbGenre>
}