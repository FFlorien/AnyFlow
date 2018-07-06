package be.florien.ampacheplayer.persistence.local.dao

import android.arch.paging.DataSource
import android.arch.persistence.db.SupportSQLiteQuery
import android.arch.persistence.room.Dao
import android.arch.persistence.room.Query
import android.arch.persistence.room.RawQuery
import be.florien.ampacheplayer.persistence.local.model.Song
import be.florien.ampacheplayer.persistence.local.model.SongDisplay
import io.reactivex.Flowable

@Dao
interface SongDao : BaseDao<Song> {

    @Query("SELECT id, title, artistName, albumName, albumArtistName, time, art FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun inQueueOrder(): DataSource.Factory<Int, SongDisplay>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    fun forPositionInQueue(position: Int): Flowable<List<Song?>>

    @RawQuery(observedEntities = [Song::class])
    fun forCurrentFilters(query: SupportSQLiteQuery): Flowable<List<Long>>

    @Query("SELECT DISTINCT genre FROM song ORDER BY genre")
    fun genreOrderByGenre(): Flowable<List<String>>
}