package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbSong
import be.florien.anyflow.data.local.model.DbSongDisplay

@Dao
interface SongDao : BaseDao<DbSong> {

    @Query("SELECT id, title, artistName, albumName, albumArtistName, time, art, url, genre FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun displayInQueueOrder(): DataSource.Factory<Int, DbSongDisplay>

    @Query("SELECT url FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun urlInQueueOrder(): LiveData<List<String>>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    suspend fun forPositionInQueue(position: Int): DbSong?

    @Query("SELECT `order` FROM queueorder WHERE queueorder.songId = :songId")
    suspend fun findPositionInQueue(songId: Long): Int?

    @RawQuery(observedEntities = [DbSong::class])
    suspend fun forCurrentFilters(query: SupportSQLiteQuery): List<Long>

    @RawQuery
    suspend fun artForFilters(query: SupportSQLiteQuery): List<String>

    @Query("SELECT DISTINCT genre FROM song ORDER BY genre COLLATE UNICODE")
    fun genreOrderByGenre(): DataSource.Factory<Int, String>

    @Query("SELECT DISTINCT genre FROM song WHERE genre LIKE :filter ORDER BY genre COLLATE UNICODE")
    fun genreOrderByGenreFiltered(filter: String): DataSource.Factory<Int, String>
}