package be.florien.ampacheplayer.persistence.local.dao

import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.room.*
import be.florien.ampacheplayer.persistence.local.model.Song
import io.reactivex.Flowable


@Dao
interface SongDao {
    @Query("SELECT * FROM song ORDER BY song.albumArtistName, song.albumName, song.track")
    fun getSong(): Flowable<List<Song>>

    @Query("SELECT * FROM song JOIN queueorder ON song.id is queueorder.songId ORDER BY queueorder.`order`")
    fun getSongsInQueueOrder(): Flowable<List<Song>>

    @RawQuery
    fun getSongsforCurrentFilters(query: SupportSQLiteQuery): List<Song>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(songs: List<Song>)

    @Update
    fun update(vararg songs: Song)

    @Delete
    fun delete(vararg songs: Song)
}