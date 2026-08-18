package be.florien.anyflow.tags.local.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import be.florien.anyflow.tags.local.model.DbMediaToPlay
import be.florien.anyflow.tags.local.model.DbQueueItem
import be.florien.anyflow.tags.local.model.DbQueueItemDisplay
import be.florien.anyflow.tags.local.model.DbQueueOrder

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

    @Query("SELECT queueorder.id as id, podcastChapter.podcastEpisodeId as podcastEpisodeId, song.local as local, mediaType, podcastChapter.startTime as startTime, podcastChapter.endTime as endTime " +
            "FROM queueorder " +
            "LEFT JOIN song ON song.id = queueorder.id " +
            "LEFT JOIN podcastchapter ON podcastchapter.id = queueorder.id " +
            "ORDER BY queueorder.`order`")
    abstract fun mediaItemsInQueueOrderUpdatable(): LiveData<List<DbMediaToPlay>>

    @Transaction
    @Query(
        "SELECT row_number() over () as position, queueorder.mediatype AS mediaType, song.id AS songId, song.title AS songTitle, artist.name AS songArtistName, album.name AS songAlbumName, album.id AS songAlbumId, song.time AS songTime, podcastepisode.id as podcastEpisodeId, podcastepisode.podcastid as podcastId, podcastepisode.title as podcastTitle, podcast.name as podcastName, podcastepisode.time as podcastTime, podcastEpisode.description as podcastDescription , podcastchapter.startTime as podcastChapterId, podcastChapter.title as podcastChapterTitle, podcastChapter.podcastEpisodeId as podcastChapterEpisodeId " +
                "FROM QueueOrder " +
                "LEFT JOIN song ON song.id = queueorder.id LEFT JOIN artist ON song.artistId = artist.id LEFT JOIN album ON song.albumId = album.id " +
                "LEFT JOIN podcastepisode on podcastepisode.id = queueorder.id LEFT JOIN podcast on podcastepisode.podcastId = podcast.id " +
                "LEFT JOIN podcastchapter on podcastChapter.id = queueorder.id " +
                "ORDER BY queueorder.`order`"
    )
    abstract fun displayInQueueOrderPaging(): DataSource.Factory<Int, DbQueueItemDisplay>
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