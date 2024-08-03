package be.florien.anyflow.feature.library.ui.list.viewmodels

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.library.domain.LibraryRepository
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryPodcastEpisodeListViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    override val navigator: Navigator,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = libraryRepository.getPodcastEpisodeFiltersPaging(filter, search) //todo handle filters & search

    override fun isThisTypeOfFilter(filter: Filter<*>): Boolean =
        filter.type == Filter.FilterType.PODCAST_EPISODE_IS

    override suspend fun getFoundFilters(
        filter: Filter<*>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            libraryRepository.getPodcastEpisodeFilterList(filter, search)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            Filter(
                Filter.FilterType.PODCAST_EPISODE_IS,
                filterValue.id,
                filterValue.displayName
            )
        )
}