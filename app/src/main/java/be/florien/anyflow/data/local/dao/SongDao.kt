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

    @Query("SELECT id, title, artistName, albumName, albumArtistName, time, art, url, genre FROM song ORDER BY title")
    fun displayInAlphabeticalOrder(): DataSource.Factory<Int, DbSongDisplay>

    @Query("SELECT id, title, artistName, albumName, albumArtistName, time, art, url, genre FROM song WHERE title LIKE :filter ORDER BY title COLLATE UNICODE")
    fun displayFiltered(filter: String): DataSource.Factory<Int, DbSongDisplay>

    @Query("SELECT id, title, artistName, albumName, albumArtistName, time, art, url, genre FROM song WHERE title LIKE :filter ORDER BY genre COLLATE UNICODE")
    suspend fun displayFilteredList(filter: String): List<DbSongDisplay>

    @Query("SELECT url FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun urlInQueueOrder(): LiveData<List<String>>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    suspend fun forPositionInQueue(position: Int): DbSong?

    @Query("SELECT `order` FROM queueorder WHERE queueorder.songId = :songId")
    suspend fun findPositionInQueue(songId: Long): Int?

    @Query("SELECT queueorder.`order` FROM queueorder JOIN song ON queueorder.songId = song.id WHERE song.title LIKE :filter OR song.artistName LIKE :filter OR song.albumArtistName LIKE :filter OR song.albumName LIKE :filter ORDER BY queueorder.`order` COLLATE UNICODE")
    fun searchPositionsWhereFilterPresent(filter: String): LiveData<List<Long>>

    @RawQuery(observedEntities = [DbSong::class])
    suspend fun forCurrentFilters(query: SupportSQLiteQuery): List<Long>

    @RawQuery
    suspend fun artForFilters(query: SupportSQLiteQuery): List<String>

    @Query("SELECT DISTINCT genre FROM song ORDER BY genre COLLATE UNICODE")
    fun genreOrderByGenre(): DataSource.Factory<Int, String>

    @Query("SELECT DISTINCT genre FROM song WHERE genre LIKE :filter ORDER BY genre COLLATE UNICODE")
    fun genreOrderByGenreFiltered(filter: String): DataSource.Factory<Int, String>

    @Query("SELECT DISTINCT genre FROM song WHERE genre LIKE :filter ORDER BY genre COLLATE UNICODE")
    suspend fun genreOrderByGenreFilteredList(filter: String): List<String>

    @Query("SELECT COUNT(*) FROM queueorder")
    suspend fun queueSize(): Int?

    @Query("SELECT * FROM song WHERE id = :songId")
    suspend fun findById(songId: Long): DbSong?
}