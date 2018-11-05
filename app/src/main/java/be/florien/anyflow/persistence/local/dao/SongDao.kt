package be.florien.anyflow.persistence.local.dao

import android.arch.paging.DataSource
import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.RawQuery
import be.florien.anyflow.persistence.local.model.Song
import be.florien.anyflow.persistence.local.model.SongDisplay
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface SongDao : BaseDao<Song> {

    @Query("SELECT id, title, artistName, albumName, albumArtistName, time, art FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun inQueueOrder(): DataSource.Factory<Int, SongDisplay>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    fun forPositionInQueue(position: Int): Maybe<Song>

    @Query("SELECT `order` FROM queueorder WHERE queueorder.songId = :songId")
    fun findPositionInQueue(songId: Long): Single<Int>

    @RawQuery(observedEntities = [Song::class])
    fun forCurrentFilters(query: SupportSQLiteQuery): Flowable<List<Long>>

    @Query("SELECT DISTINCT genre FROM song ORDER BY genre COLLATE UNICODE")
    fun genreOrderByGenre(): Flowable<List<String>>
}