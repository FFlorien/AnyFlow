package be.florien.ampacheplayer.persistence.local.dao

import android.arch.paging.DataSource
import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Song
import io.reactivex.Flowable


@Dao
interface SongDao {
    @Query("SELECT DISTINCT genre FROM song ORDER BY genre")
    fun getSongsGenre(): Flowable<List<String>>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun getSongsInQueueOrder(): DataSource.Factory<Int, Song>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    fun getSongForPositionInQueue(position: Int): List<Song>

    @RawQuery(observedEntities = [Song::class])
    fun getSongsForCurrentFilters(query: SupportSQLiteQuery): Flowable<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(songs: List<Song>)

    @Update
    fun update(vararg songs: Song)

    @Delete
    fun delete(vararg songs: Song)
}