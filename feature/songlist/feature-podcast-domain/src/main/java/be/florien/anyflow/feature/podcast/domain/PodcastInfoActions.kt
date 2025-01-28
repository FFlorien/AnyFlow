package be.florien.anyflow.feature.podcast.domain

import android.content.SharedPreferences
import be.florien.anyflow.feature.podcast.base.domain.BasePodcastInfoActions
import be.florien.anyflow.feature.podcast.base.domain.model.BasePodcastInfoRow
import be.florien.anyflow.feature.podcast.base.domain.model.PodcastFieldType
import be.florien.anyflow.management.download.DownloadManager
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.PodcastFilterType
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import be.florien.anyflow.management.queue.OrderComposer
import javax.inject.Inject
import javax.inject.Named

class PodcastInfoActions @Inject constructor(
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val downloadManager: DownloadManager,
    @Named("preferences") sharedPreferences: SharedPreferences
) : BasePodcastInfoActions(sharedPreferences) {

    /**
     * Action methods
     */

    suspend fun playNext(songId: Long) {
        orderComposer.changeSongPositionForNext(songId)
    }

    suspend fun filterOn(podcastEpisode: PodcastEpisodeDisplay, row: BasePodcastInfoRow) {
        val filter = when (row.fieldType) {
            PodcastFieldType.Title -> Filter(
                PodcastFilterType.PODCAST_EPISODE_IS,
                podcastEpisode.id,
                podcastEpisode.title
            )

            PodcastFieldType.Podcast -> Filter(
                PodcastFilterType.PODCAST_IS,
                podcastEpisode.podcastId,
                podcastEpisode.podcast
            )

//            PodcastFieldType.State -> Filter( todo
//                PodcastFilterType.STATE_IS,
//                podcastEpisode.albumId,
//                podcastEpisode.albumName
//            )

            else -> throw IllegalArgumentException("This field can't be filtered on")
        }
        filtersManager.clearFilters()
        filtersManager.addFilter(filter)
        filtersManager.commitChanges()
    }

    fun getSearchTerms(podcastEpisode: PodcastEpisodeDisplay, fieldType: PodcastFieldType): String {
        return when (fieldType) {
            PodcastFieldType.Title -> podcastEpisode.title
            PodcastFieldType.Podcast -> podcastEpisode.podcast
            else -> throw IllegalArgumentException("This field can't be searched on")
        }
    }

    fun queueDownload(songInfo: PodcastEpisodeDisplay, fieldType: PodcastFieldType, index: Int?) {
        val data = when (fieldType) {
            PodcastFieldType.Title -> Triple(
                songInfo.id,
                PodcastFilterType.PODCAST_EPISODE_IS,
                -1
            )

            PodcastFieldType.Podcast -> Triple(
                songInfo.podcastId,
                PodcastFilterType.PODCAST_IS,
                -1
            )

            else -> return
        }
        downloadManager.queueDownload(data.first, data.second, data.third)
    }
}