package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbMediaToPlay
import be.florien.anyflow.data.local.model.DbQueueItemDisplay
import be.florien.anyflow.data.local.model.DbQueueOrder
import be.florien.anyflow.data.local.model.DbSong

@Dao
abstract class QueueOrderDao : BaseDao<DbQueueOrder>() {
    @Transaction
    @Query(
        "SELECT queueorder.mediatype AS mediaType, song.id AS songId, song.title AS songTitle, artist.name AS songArtistName, album.name AS songAlbumName, album.id AS songAlbumId, song.time AS songTime, podcastepisode.id as podcastEpisodeId, podcastepisode.podcastid as podcastId, podcastepisode.title as podcastTitle, podcastepisode.authorFull as podcastAuthor, podcast.name as podcastName, podcastepisode.time as podcastTime " +
                "FROM song JOIN artist ON song.artistId = artist.id JOIN album ON song.albumId = album.id JOIN queueorder ON song.id = queueorder.id LEFT JOIN podcastepisode on podcastepisode.id = queueorder.id LEFT JOIN podcast on podcastepisode.id = podcast.id " +
                "ORDER BY queueorder.`order`"
    )
    abstract fun displayInQueueOrder(): DataSource.Factory<Int, DbQueueItemDisplay>

    @Query("SELECT queueorder.id as id, song.local as local, mediaType " +
            "FROM queueorder JOIN song ON song.id = queueorder.id " +
            "ORDER BY queueorder.`order`")
    abstract fun mediaItemsInQueueOrder(): LiveData<List<DbMediaToPlay>>

    @RawQuery(observedEntities = [DbSong::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbQueueItemDisplay>

    @RawQuery(observedEntities = [DbSong::class])
    abstract suspend fun rawQueryListDisplay(query: SupportSQLiteQuery): List<DbQueueItemDisplay>

    @Query("SELECT count(*) FROM queueorder")
    protected abstract suspend fun getCount(): Int

    @Query("DELETE FROM queueorder")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun setOrder(orderList: List<DbQueueOrder>) {
        deleteAll()
        insert(orderList)
    }
}