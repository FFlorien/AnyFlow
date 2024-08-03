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

class LibraryPlaylistListViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    override val navigator: Navigator,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        libraryRepository.getPlaylistFiltersPaging(filter, search)

    override fun isThisTypeOfFilter(filter: Filter<*>) =
        filter.type == Filter.FilterType.PLAYLIST_IS

    override suspend fun getFoundFilters(
        filter: Filter<*>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            libraryRepository.getPlaylistFilterList(filter, search)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            Filter(
                Filter.FilterType.PLAYLIST_IS,
                filterValue.id,
                filterValue.displayName
            )
        )
}