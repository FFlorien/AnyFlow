package be.florien.anyflow.feature.library.podcast.ui.list.viewmodels

import androidx.paging.PagingData
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.podcast.domain.LibraryPodcastRepository
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.PodcastFilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryPodcastListViewModel @Inject constructor(
    private val libraryTagsRepository: LibraryPodcastRepository,
    override val navigator: Navigator,
    filtersManager: FiltersManager,
    authenticationInterceptor: AuthenticationInterceptor
) : LibraryListViewModel(filtersManager,authenticationInterceptor) {
    override fun getPagingList(
        filter: Filter?,
        search: String?
    ): Flow<PagingData<FilterItem>> = libraryTagsRepository.getPodcastFiltersPaging(filter, search) //todo handle search

    override fun isThisTypeOfFilter(filterParam: FilterParam<*>): Boolean =
        filterParam.type == PodcastFilterType.PODCAST_IS

    override suspend fun getFoundFilters(
        filter: Filter?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            libraryTagsRepository.getPodcastFilterList(filter, search)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            FilterParam(
                PodcastFilterType.PODCAST_IS,
                filterValue.id,
                filterValue.title
            )
        )
}