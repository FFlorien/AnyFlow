package be.florien.anyflow.feature.library.podcast.ui.list.viewmodels

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastRepository
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import be.florien.anyflow.management.filters.model.PodcastFilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryPodcastListViewModel @Inject constructor(
    private val libraryTagsRepository: LibraryPodcastRepository,
    override val navigator: Navigator,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = libraryTagsRepository.getPodcastFiltersPaging(filter, search) //todo handle search

    override fun isThisTypeOfFilter(filter: Filter<*>): Boolean =
        filter.type == PodcastFilterType.PODCAST_IS

    override suspend fun getFoundFilters(
        filter: Filter<*>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            libraryTagsRepository.getPodcastFilterList(filter, search)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            Filter(
                PodcastFilterType.PODCAST_IS,
                filterValue.id,
                filterValue.title.getText()
            )
        )
}