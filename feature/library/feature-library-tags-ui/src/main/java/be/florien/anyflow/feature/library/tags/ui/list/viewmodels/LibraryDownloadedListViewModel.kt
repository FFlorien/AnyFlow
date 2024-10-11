package be.florien.anyflow.feature.library.tags.ui.list.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.PagingData
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.domain.model.FilterItem
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsRepository
import be.florien.anyflow.feature.library.ui.R
import be.florien.anyflow.feature.library.ui.list.LibraryListViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter
import javax.inject.Inject

class LibraryDownloadedListViewModel @Inject constructor(
    private val libraryTagsRepository: LibraryTagsRepository,//todo get stats from libraryRepository
    override val navigator: Navigator,
    filtersManager: FiltersManager,
    context: Context
) : LibraryListViewModel(filtersManager) {

    private val downloadedName = context.getString(R.string.filter_is_downloaded)
    private val notDownloadedName = context.getString(R.string.filter_is_not_downloaded)
    override val hasSearch = false

    override fun getPagingList(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = libraryTagsRepository.getDownloadedFiltersPaging(
        filter, downloadedName, notDownloadedName
    )

    override fun isThisTypeOfFilter(filter: Filter<*>) =
        filter.type == Filter.FilterType.DOWNLOADED_STATUS_IS

    override suspend fun getFoundFilters(
        filter: Filter<*>?,
        search: String
    ): List<FilterItem> =
        libraryTagsRepository.getDownloadedFiltersList(filter, downloadedName, notDownloadedName)

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(Filter(Filter.FilterType.DOWNLOADED_STATUS_IS, filterValue.id == 1L, ""))

}