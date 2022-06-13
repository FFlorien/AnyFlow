package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import be.florien.anyflow.data.local.model.DbGenre

@Dao
abstract class GenreDao : BaseDao<DbGenre>() {
    @Query("SELECT * FROM genre ORDER BY name COLLATE UNICODE")
    abstract fun genreOrderByGenre(): DataSource.Factory<Int, DbGenre>

    @Query("SELECT * FROM genre WHERE name LIKE :filter ORDER BY name COLLATE UNICODE")
    abstract fun genreOrderByGenreFiltered(filter: String): DataSource.Factory<Int, DbGenre>

    @Query("SELECT * FROM genre WHERE name LIKE :filter ORDER BY name COLLATE UNICODE")
    abstract suspend fun genreOrderByGenreFilteredList(filter: String): List<DbGenre>
}