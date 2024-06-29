package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.*

@Dao
abstract class SongDao : BaseDao<DbSong>() {

    // region SELECT
    // Single items
    @Transaction
    @Query("SELECT * FROM song WHERE song.id = :songId")
    abstract suspend fun songById(songId: Long): DbSongInfo

    @Query("SELECT waveForm FROM Song WHERE song.id = :songId")
    abstract suspend fun getWaveForm(songId: Long): DbSongWaveForm?

    @RawQuery(observedEntities = [DbSong::class])
    abstract suspend fun rawQueryForCountFiltered(query: SupportSQLiteQuery): Int

    @Query("SELECT COUNT(*) FROM song")
    abstract suspend fun songCount(): Int

    @Query("SELECT time FROM Song WHERE song.id = :songId")
    abstract suspend fun getSongDuration(songId: Long): Int

    // Lists
    @RawQuery(observedEntities = [DbSong::class])
    abstract suspend fun rawQueryListDisplay(query: SupportSQLiteQuery): List<DbSongDisplay>

    @Query("SELECT song.id as id, song.local as local, $SONG_MEDIA_TYPE as mediaType FROM song WHERE song.id IN (:ids)")
    abstract suspend fun songsToUpdate(ids: List<Long>): List<DbMediaToPlay>

    @RawQuery(observedEntities = [DbSong::class])
    abstract suspend fun forCurrentFiltersList(query: SupportSQLiteQuery): List<Long>

    // Updatable
    @Transaction
    @Query("SELECT * FROM song WHERE song.id = :songId")
    abstract fun songByIdUpdatable(songId: Long): LiveData<DbSongInfo>

    @Query("SELECT queueorder.`order` " +
            "FROM queueorder JOIN song ON queueorder.id = song.id JOIN artist ON song.artistId = artist.id JOIN album ON song.albumId = album.id JOIN artist AS albumArtist ON album.artistId = albumArtist.id " +
            "WHERE song.title LIKE :filter OR artist.name LIKE :filter OR albumArtist.name LIKE :filter OR album.name LIKE :filter " +
            "ORDER BY queueorder.`order` COLLATE UNICODE")
    abstract fun searchPositionsWhereFilterPresentUpdatable(filter: String): LiveData<List<Long>>

    @Query("SELECT waveForm FROM Song WHERE song.id = :songId")
    abstract fun getWaveFormUpdatable(songId: Long): LiveData<DbSongWaveForm>

    // Paging
    @RawQuery(observedEntities = [DbSong::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbSongDisplay>
    // endregion

    // region INSERT
    @Query("UPDATE song SET waveForm = :downSamples WHERE song.id = :songId")
    abstract suspend fun updateWithNewWaveForm(songId: Long, downSamples: String?)

    @Query("UPDATE song SET local = :uri WHERE song.id = :songId")
    abstract suspend fun updateWithLocalUri(songId: Long, uri: String?)
    // endregion

    @Delete(entity = DbSong::class)
    abstract suspend fun deleteWithId(ids: List<DbSongId>)

}