package be.florien.anyflow.feature.player.filter.selection

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingData
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.filter.FilterActions
import javax.inject.Inject

class SelectFilterDownloadedViewModel @Inject constructor(
    val dataRepository: DataRepository,
    filterActions: FilterActions,
    context: Context
) : SelectFilterViewModel(filterActions) {

    override val hasSearch = false

    private val downloadedName = context.getString(R.string.filter_is_downloaded)
    private val notDownloadedName = context.getString(R.string.filter_is_not_downloaded)
    private var currentDownloadFilters: List<Filter.DownloadedStatusIs> = listOf()
    private val liveData = MutableLiveData(PagingData.from(getFilterList()))
    init {
        filtersManager.filtersInEdition.observeForever {
            currentDownloadFilters = it.filterIsInstance<Filter.DownloadedStatusIs>()
            liveData.value = PagingData.from(getFilterList())
        }
    }


    override fun getUnfilteredPagingList() = liveData
    override fun getFilteredPagingList(search: String) = liveData
    override fun isThisTypeOfFilter(filter: Filter<*>) = filter is Filter.DownloadedStatusIs

    override suspend fun getFoundFilters(search: String): List<FilterItem> = getFilterList()

    override fun getFilter(filterValue: FilterItem) =
        Filter.DownloadedStatusIs(filterValue.id == 0L)

    private fun getFilterList() = listOf(
        FilterItem(0, downloadedName, null, currentDownloadFilters.any { it.argument }),
        FilterItem(1, notDownloadedName, null, currentDownloadFilters.any { !it.argument })
    )

}