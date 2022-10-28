package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.*

@Dao
abstract class SongDao : BaseDao<DbSong>() {

    // GETTERS

    // DataSources
    @Transaction
    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    abstract fun displayInQueueOrder(): DataSource.Factory<Int, DbSongDisplay>

    @Transaction
    @Query("SELECT * FROM song ORDER BY title")
    abstract fun displayInAlphabeticalOrder(): DataSource.Factory<Int, DbSongDisplay>

    @Transaction
    @Query("SELECT * FROM song WHERE title LIKE :filter ORDER BY title COLLATE UNICODE")
    abstract fun displayFiltered(filter: String): DataSource.Factory<Int, DbSongDisplay>

    // List of songs
    @Transaction
    @Query("SELECT * FROM song WHERE title LIKE :filter ORDER BY title COLLATE UNICODE")
    abstract suspend fun displayFilteredList(filter: String): List<DbSongDisplay>

    @Query("SELECT song.id, song.local FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    abstract fun songsInQueueOrder(): LiveData<List<DbSongToPlay>>

    // Song
    @Transaction
    @Query("SELECT song.id, song.title, song.artistId, song.albumId, song.track, song.disk, song.time, song.year, song.composer, song.local, song.downSamples FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    abstract suspend fun forPositionInQueue(position: Int): DbSongDisplay?

    @Transaction
    @Query("SELECT * FROM song WHERE song.id = :songId")
    abstract suspend fun findById(songId: Long): DbSongDisplay?

    // Related to queue or filter
    @Query("SELECT `order` FROM queueorder WHERE queueorder.songId = :songId")
    abstract suspend fun findPositionInQueue(songId: Long): Int?

    @Query("SELECT COUNT(*) FROM queueorder")
    abstract suspend fun queueSize(): Int?

    @Query("SELECT queueorder.`order` FROM queueorder JOIN song ON queueorder.songId = song.id JOIN artist ON song.artistId = artist.id JOIN album ON song.albumId = album.id JOIN artist AS albumArtist ON album.artistId = albumArtist.id WHERE song.title LIKE :filter OR artist.name LIKE :filter OR albumArtist.name LIKE :filter OR album.name LIKE :filter ORDER BY queueorder.`order` COLLATE UNICODE")
    abstract fun searchPositionsWhereFilterPresent(filter: String): LiveData<List<Long>>

    @RawQuery(observedEntities = [DbSong::class])
    abstract suspend fun forCurrentFilters(query: SupportSQLiteQuery): List<Long>

    //Data
    @Query("SELECT COUNT(*) FROM song")
    abstract suspend fun songCount(): Int

    @Query("SELECT downSamples FROM Song WHERE song.id = :songId")
    abstract suspend fun getDownSamples(songId: Long): DbSongDownSample

    @Query("SELECT time FROM Song WHERE song.id = :songId")
    abstract suspend fun getSongDuration(songId: Long): Int

    //SETTERS

    @Delete(entity = DbSong::class)
    abstract suspend fun deleteWithId(ids: List<DbSongId>)

    @Query("UPDATE song SET local = :uri WHERE song.id = :songId")
    abstract suspend fun updateWithLocalUri(songId: Long, uri: String)

    @Query("UPDATE song SET downSamples = :downSamples WHERE song.id = :songId")
    abstract suspend fun updateWithNewDownSamples(songId: Long, downSamples: String?)

}