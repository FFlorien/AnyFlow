package be.florien.anyflow.feature.library.podcast.domain

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.common.di.ServerScope
import be.florien.anyflow.common.management.convertToPagingLiveData
import be.florien.anyflow.management.podcast.PodcastRepository
import be.florien.anyflow.tags.UrlRepository
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import javax.inject.Inject

@ServerScope
class LibraryPodcastRepository @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val urlRepository: UrlRepository,
    private val filtersManager: FiltersManager
) {
    // region paging

    fun getPodcastEpisodeFiltersPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        podcastRepository
            .getAllPodcastsEpisodes()
            .map { it.toFilterItem(filter, urlRepository, filtersManager) }
            .convertToPagingLiveData()
    //endregion

    //region Filter list
    suspend fun getPodcastEpisodeFilterList(
        filter: Filter<*>?,
        search: String
    ) = podcastRepository
        .getAllPodcastsEpisodesList()
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }
    //endregion

    //region Display list
    suspend fun getPodcastEpisodeList(filter: Filter<*>?) =
        podcastRepository
            .getAllPodcastsEpisodesList()
            .map(PodcastEpisodeDisplay::toDisplayData)
    //endregion

    suspend fun getFilteredInfo(infoSource: Filter<*>?) = podcastRepository.getFilteredInfo(infoSource)

    fun getArtUrl(artType: String?, argument: Long): String? =
        if (artType == null) {
            null
        } else {
            urlRepository.getArtUrl(artType, argument)
        }
}