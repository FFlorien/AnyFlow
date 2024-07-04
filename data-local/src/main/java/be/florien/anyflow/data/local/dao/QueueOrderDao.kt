package be.florien.anyflow.data.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import be.florien.anyflow.data.local.model.DbMediaToPlay
import be.florien.anyflow.data.local.model.DbPodcastEpisode
import be.florien.anyflow.data.local.model.DbQueueItem
import be.florien.anyflow.data.local.model.DbQueueItemDisplay
import be.florien.anyflow.data.local.model.DbQueueOrder
import be.florien.anyflow.data.local.model.DbSong

@Dao
abstract class QueueOrderDao : BaseDao<DbQueueOrder>() {
    // region SELECT
    @Query("SELECT count(*) FROM queueorder")
    protected abstract suspend fun getCount(): Int

    @Query("SELECT `order` FROM queueorder WHERE queueorder.id = :songId")
    abstract suspend fun findPositionInQueue(songId: Long): Int?

    @Transaction
    @Query("SELECT queueorder.id AS id, queueorder.mediaType AS mediaType " +
            "FROM queueorder " +
            "WHERE queueorder.`order` = :position")
    abstract suspend fun queueItemInPosition(position: Int): DbQueueItem?

    @Query("SELECT queueorder.id as id, song.local as local, mediaType " +
            "FROM queueorder " +
            "LEFT JOIN song ON song.id = queueorder.id " +
            "ORDER BY queueorder.`order`")
    abstract fun mediaItemsInQueueOrderUpdatable(): LiveData<List<DbMediaToPlay>>

    @Transaction
    @Query(
        "SELECT queueorder.mediatype AS mediaType, song.id AS songId, song.title AS songTitle, artist.name AS songArtistName, album.name AS songAlbumName, album.id AS songAlbumId, song.time AS songTime, podcastepisode.id as podcastEpisodeId, podcastepisode.podcastid as podcastId, podcastepisode.title as podcastTitle, podcastepisode.authorFull as podcastAuthor, podcast.name as podcastName, podcastepisode.time as podcastTime " +
                "FROM QueueOrder " +
                "LEFT JOIN song ON song.id = queueorder.id LEFT JOIN artist ON song.artistId = artist.id LEFT JOIN album ON song.albumId = album.id " +
                "LEFT JOIN podcastepisode on podcastepisode.id = queueorder.id LEFT JOIN podcast on podcastepisode.id = podcast.id " +
                "ORDER BY queueorder.`order`"
    )
    abstract fun displayInQueueOrderPaging(): DataSource.Factory<Int, DbQueueItemDisplay>

    @RawQuery(observedEntities = [DbQueueOrder::class, DbSong::class, DbPodcastEpisode::class])
    abstract fun rawQueryPaging(query: SupportSQLiteQuery): DataSource.Factory<Int, DbQueueItemDisplay>
    //endregion

    //region INSERT
    @Transaction
    open suspend fun setOrder(orderList: List<DbQueueOrder>) {
        deleteAll()
        insertList(orderList)
    }
    // endregion

    // region DELETE
    @Query("DELETE FROM queueorder")
    abstract suspend fun deleteAll()
    // endregion
}