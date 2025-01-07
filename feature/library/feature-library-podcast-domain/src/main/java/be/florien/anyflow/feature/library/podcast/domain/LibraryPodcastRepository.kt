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
import be.florien.anyflow.management.podcast.model.PodcastDisplay
import be.florien.anyflow.management.podcast.model.PodcastEpisodeDisplay
import javax.inject.Inject

@ServerScope
class LibraryPodcastRepository @Inject constructor(
    private val podcastRepository: PodcastRepository,
    private val urlRepository: UrlRepository,
    private val filtersManager: FiltersManager
) {
    // region paging

    fun getPodcastFiltersPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        podcastRepository
            .getPodcasts(filter?.let { listOf(it) }, search)
            .map { it.toFilterItem(filter, urlRepository, filtersManager) }
            .convertToPagingLiveData()

    fun getPodcastEpisodeFiltersPaging(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        podcastRepository
            .getPodcastsEpisodes(filter?.let { listOf(it) }, search)
            .map { it.toFilterItem(filter, urlRepository, filtersManager) }
            .convertToPagingLiveData()
    //endregion

    //region Filter list
    suspend fun getPodcastFilterList(
        filter: Filter<*>?,
        search: String
    ) = podcastRepository
        .getAllPodcastsList()
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }

    suspend fun getPodcastEpisodeFilterList(
        filter: Filter<*>?,
        search: String
    ) = podcastRepository
        .getAllPodcastsEpisodesList()
        .map { it.toFilterItem(filter, urlRepository, filtersManager) }
    //endregion

    //region Display list
    suspend fun getPodcastList(filter: Filter<*>?) =
        podcastRepository
            .getAllPodcastsList()
            .map(PodcastDisplay::toIdText)

    suspend fun getPodcastEpisodeList(filter: Filter<*>?) =
        podcastRepository
            .getAllPodcastsEpisodesList()
            .map(PodcastEpisodeDisplay::toIdText)
    //endregion

    suspend fun getFilteredInfo(infoSource: Filter<*>?) = podcastRepository.getFilteredInfo(infoSource)

    fun getArtUrl(artType: String?, argument: Long): String? =
        if (artType == null) {
            null
        } else {
            urlRepository.getArtUrl(artType, argument)
        }
}