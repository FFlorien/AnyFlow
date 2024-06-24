package be.florien.anyflow.feature.player.ui.library.list.viewmodels

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.data.PodcastRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.local.model.DbPodcastEpisode
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.player.ui.library.list.LibraryListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryPodcastEpisodeListViewModel @Inject constructor(
    val dataRepository: PodcastRepository,
    val urlRepository: UrlRepository,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(
        filters: List<Filter<*>>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = dataRepository.getAllPodcastsEpisodes(::convert) //todo handle filters & search

    override fun isThisTypeOfFilter(filter: Filter<*>): Boolean =
        filter.type == Filter.FilterType.PODCAST_EPISODE_IS

    override suspend fun getFoundFilters(
        filters: List<Filter<*>>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            dataRepository.getAllPodcastsEpisodesList(::convert)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            Filter(
                Filter.FilterType.PODCAST_EPISODE_IS,
                filterValue.id,
                filterValue.displayName
            )
        )

    private fun convert(podcastEpisode: DbPodcastEpisode): FilterItem {
        val filter = getFilterInParent(Filter(Filter.FilterType.PODCAST_EPISODE_IS,
            podcastEpisode.id, podcastEpisode.title))
        return (FilterItem(
            podcastEpisode.id,
            "${podcastEpisode.title}\nby ${podcastEpisode.authorFull}", //todo wut ? i18n ?  + from ${podcastEpisode.albumName}
            filtersManager.isFilterInEdition(filter),
            urlRepository.getArtUrl("podcast", podcastEpisode.podcastId),
        ))
    }
}