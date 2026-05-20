package be.florien.anyflow.feature.library.ui.list.viewmodels

import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.library.tags.domain.LibraryListTagsRepository
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryAlbumListViewModel @Inject constructor(
    private val libraryListTagsRepository: LibraryListTagsRepository,
    override val navigator: Navigator,
    filtersManager: FiltersManager,
    authenticationInterceptor: AuthenticationInterceptor
) : LibraryListViewModel(filtersManager,authenticationInterceptor) {
    override fun getPagingList(filter: Filter?, search: String?) =
        libraryListTagsRepository.getAlbumFiltersPaging(filter, search)

    override fun isThisTypeOfFilter(filterParam: FilterParam<*>) = filterParam.type == TagFilterType.ALBUM_IS

    override suspend fun getFoundFilters(
        filter: Filter?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            libraryListTagsRepository.getAlbumFilterList(filter, search)
        }

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(
            FilterParam(
                TagFilterType.ALBUM_IS,
                filterValue.id,
                filterValue.title
            )
        )
}