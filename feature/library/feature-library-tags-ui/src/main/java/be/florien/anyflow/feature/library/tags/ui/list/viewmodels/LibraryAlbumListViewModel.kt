package be.florien.anyflow.feature.library.tags.ui.list.viewmodels

import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsRepository
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryAlbumListViewModel @Inject constructor(
    private val libraryTagsRepository: LibraryTagsRepository,
    override val navigator: Navigator,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(filter: Filter<*>?, search: String?) =
        libraryTagsRepository.getAlbumFiltersPaging(filter, search)

    override fun isThisTypeOfFilter(filter: Filter<*>) = filter.type == Filter.FilterType.ALBUM_IS

    override suspend fun getFoundFilters(
        filter: Filter<*>?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            libraryTagsRepository.getAlbumFilterList(filter, search)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            Filter(
                Filter.FilterType.ALBUM_IS,
                filterValue.id,
                filterValue.displayName,
                emptyList()
            )
        )
}