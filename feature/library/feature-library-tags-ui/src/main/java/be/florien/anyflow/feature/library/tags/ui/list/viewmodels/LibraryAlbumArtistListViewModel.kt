package be.florien.anyflow.feature.library.tags.ui.list.viewmodels

import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsRepository
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.management.filters.domain.model.FilterParam
import be.florien.anyflow.management.filters.domain.model.TagFilterType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LibraryAlbumArtistListViewModel @Inject constructor(
    private val libraryTagsRepository: LibraryTagsRepository,
    override val navigator: Navigator,
    filtersManager: FiltersManager
) : LibraryListViewModel(filtersManager) {
    override fun getPagingList(
        filter: Filter?,
        search: String?
    ): LiveData<PagingData<FilterItem>> =
        libraryTagsRepository.getAlbumArtistsPaging(filter, search)

    override fun isThisTypeOfFilter(filterParam: FilterParam<*>) =
        filterParam.type == TagFilterType.ALBUM_ARTIST_IS

    override suspend fun getFoundFilters(
        filter: Filter?,
        search: String
    ): List<FilterItem> =
        withContext(Dispatchers.Default) {
            libraryTagsRepository.getAlbumArtistFilterList(filter, search)
        }

    override fun getFilter(filterValue: FilterItem): Filter {
        val filterParam = FilterParam(
            TagFilterType.ALBUM_ARTIST_IS,
            filterValue.id,
            filterValue.title.getText()
        )
        return getFilterInParent(filterParam)
    }
}