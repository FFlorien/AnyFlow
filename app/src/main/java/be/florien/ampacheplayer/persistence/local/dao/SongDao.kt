package be.florien.ampacheplayer.persistence.local.dao

import android.arch.paging.DataSource
import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Song
import io.reactivex.Flowable

@Dao
interface SongDao : BaseDao<Song> {

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun inQueueOrder(): DataSource.Factory<Int, Song>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    fun forPositionInQueue(position: Int): List<Song>

    @RawQuery(observedEntities = [Song::class])
    fun forCurrentFilters(query: SupportSQLiteQuery): Flowable<List<Song>>

    @Query("SELECT DISTINCT genre FROM song ORDER BY genre")
    fun genreOrderedByGenre(): Flowable<List<String>>
}