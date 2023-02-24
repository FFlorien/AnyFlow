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
    @Query("SELECT id, title, artistId, albumId, track, disk, time, year, composer, size, local, waveForm FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    abstract fun displayInQueueOrder(): DataSource.Factory<Int, DbSongDisplay>

    @RawQuery(observedEntities = [DbSong::class])
    abstract suspend fun rawQueryList(query: SupportSQLiteQuery): List<DbSongDisplay>

    // List of songs

    @Query("SELECT song.id, song.local FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    abstract fun songsInQueueOrder(): LiveData<List<DbSongToPlay>>

    @RawQuery(observedEntities = [DbSongDisplay::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbSongDisplay>

    // Song
    @Transaction
    @Query("SELECT song.id, song.title, song.artistId, song.albumId, song.track, song.disk, song.time, song.year, song.composer, song.size, song.local, song.waveForm FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    abstract suspend fun forPositionInQueue(position: Int): DbSongDisplay?

    @Transaction
    @Query("SELECT * FROM song WHERE song.id = :songId")
    abstract fun findById(songId: Long): LiveData<DbSongDisplay>

    @Transaction
    @Query("SELECT * FROM song WHERE song.id = :songId")
    abstract suspend fun findByIdSync(songId: Long): DbSongDisplay

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

    @Query("SELECT waveForm FROM Song WHERE song.id = :songId")
    abstract suspend fun getWaveForm(songId: Long): DbSongWaveForm

    @Query("SELECT time FROM Song WHERE song.id = :songId")
    abstract suspend fun getSongDuration(songId: Long): Int

    //SETTERS

    @Delete(entity = DbSong::class)
    abstract suspend fun deleteWithId(ids: List<DbSongId>)

    @Query("UPDATE song SET local = :uri WHERE song.id = :songId")
    abstract suspend fun updateWithLocalUri(songId: Long, uri: String?)

    @Query("UPDATE song SET waveForm = :downSamples WHERE song.id = :songId")
    abstract suspend fun updateWithNewWaveForm(songId: Long, downSamples: String?)

}