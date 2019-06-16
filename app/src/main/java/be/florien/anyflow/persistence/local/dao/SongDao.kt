package be.florien.anyflow.persistence.local.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.persistence.local.model.Song
import be.florien.anyflow.persistence.local.model.SongDisplay
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
interface SongDao : BaseDao<Song> {

    @Query("SELECT id, title, artistName, albumName, albumArtistName, filename, url,  time, art FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun displayInQueueOrder(): DataSource.Factory<Int, SongDisplay>

    @Query("SELECT url FROM song JOIN queueorder ON song.id = queueorder.songId ORDER BY queueorder.`order`")
    fun urlInQueueOrder(): Flowable<List<String>>

    @Query("SELECT * FROM song JOIN queueorder ON song.id = queueorder.songId WHERE queueorder.`order` = :position")
    fun forPositionInQueue(position: Int): Maybe<Song>

    @Query("SELECT `order` FROM queueorder WHERE queueorder.songId = :songId")
    fun findPositionInQueue(songId: Long): Maybe<Int>

    @RawQuery(observedEntities = [Song::class])
    fun forCurrentFilters(query: SupportSQLiteQuery): Flowable<List<Long>>

    @RawQuery
    fun artForFilters(query: SupportSQLiteQuery): List<String>

    @Query("SELECT DISTINCT genre FROM song ORDER BY genre COLLATE UNICODE")
    fun genreOrderByGenre(): DataSource.Factory<Int, String>

    @Query("SELECT id, title, artistName, albumName, albumArtistName, filename, url,  time, art FROM song WHERE downloadStatus = :downloadStatus")
    fun forDownloadStatus(downloadStatus: Int): Single<List<SongDisplay>>

    @Query("UPDATE song SET localFileName = :localFileName, downloadStatus = :newStatus WHERE song.id = :id")
    fun updateWithLocalFilename(id: Long, localFileName: String, newStatus: Int)

    @Query("UPDATE song SET downloadStatus = :newStatus WHERE song.id = :id")
    fun updateDownloadedStatus(id: Long, newStatus: Int)

    @Query("SELECT id, title, artistName, albumName, albumArtistName, filename, url,  time, art FROM song WHERE albumId = :albumId")
    fun getSongsForAlbum(albumId: Long): Single<List<SongDisplay>>

    @Query("SELECT id, title, artistName, albumName, albumArtistName, filename, url,  time, art FROM song WHERE artistId = :artistId")
    fun getSongsForArtist(artistId: Long): Single<List<SongDisplay>>

    @Query("SELECT id, title, artistName, albumName, albumArtistName, filename, url,  time, art FROM song WHERE genre like :genre")
    fun getSongsForGenre(genre: String): Single<List<SongDisplay>>
}