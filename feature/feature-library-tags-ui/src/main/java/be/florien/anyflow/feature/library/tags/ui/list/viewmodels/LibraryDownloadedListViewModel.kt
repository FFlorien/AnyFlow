package be.florien.anyflow.feature.library.tags.ui.list.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.library.tags.domain.LibraryTagsRepository
import be.florien.anyflow.feature.library.domain.model.FilterItem
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

    override val hasSearch = false
    override fun getPagingList(
        filter: Filter<*>?,
        search: String?
    ): LiveData<PagingData<FilterItem>> = liveData

    private val downloadedName = context.getString(R.string.filter_is_downloaded)
    private val notDownloadedName = context.getString(R.string.filter_is_not_downloaded)
    private var currentDownloadFilters: List<Filter<Boolean>> = listOf()
    private val liveData = MutableLiveData(PagingData.from(getFilterList()))

    init {
        filtersManager.filtersInEdition.observeForever {
            currentDownloadFilters = it.filterIsInstance<Filter<Boolean>>()
            liveData.value = PagingData.from(getFilterList())
        }
    }

    override fun isThisTypeOfFilter(filter: Filter<*>) =
        filter.type == Filter.FilterType.DOWNLOADED_STATUS_IS

    override suspend fun getFoundFilters(
        filter: Filter<*>?,
        search: String
    ): List<FilterItem> = getFilterList()

    override fun getFilter(filterValue: FilterItem) =
        getFilterInParent(Filter(Filter.FilterType.DOWNLOADED_STATUS_IS, filterValue.id == 0L, ""))

    private fun getFilterList() = listOf(
        FilterItem(0, downloadedName, currentDownloadFilters.any { it.argument }),
        FilterItem(1, notDownloadedName, currentDownloadFilters.any { !it.argument })
    )

}