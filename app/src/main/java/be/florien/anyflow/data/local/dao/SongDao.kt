package be.florien.anyflow.data.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbSong
import be.florien.anyflow.data.local.model.DbSongDisplay
import io.reactivex.Flowable
import io.reactivex.Maybe

@Dao
interface SongDao : BaseDao<DbSong> {

    @Query("SELECT id, title, artistName, albumName, albumArtistName, time, art, url, genre FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun displayInQueueOrder(): DataSource.Factory<Int, DbSongDisplay>

    @Query("SELECT url FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun urlInQueueOrder(): Flowable<List<String>>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    fun forPositionInQueue(position: Int): Maybe<DbSong>

    @Query("SELECT `order` FROM queueorder WHERE queueorder.songId = :songId")
    fun findPositionInQueue(songId: Long): Maybe<Int>

    @RawQuery(observedEntities = [DbSong::class])
    fun forCurrentFilters(query: SupportSQLiteQuery): Flowable<List<Long>>

    @RawQuery
    fun artForFilters(query: SupportSQLiteQuery): List<String>

    @Query("SELECT DISTINCT genre FROM song ORDER BY genre COLLATE UNICODE")
    fun genreOrderByGenre(): DataSource.Factory<Int, String>
}