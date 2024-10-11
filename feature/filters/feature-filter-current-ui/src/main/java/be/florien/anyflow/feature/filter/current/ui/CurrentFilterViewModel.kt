package be.florien.anyflow.feature.filter.current.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.tags.UrlRepository
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import javax.inject.Inject

class CurrentFilterViewModel @Inject constructor(
    override val filtersManager: FiltersManager,
    val urlRepository: UrlRepository,
    override val navigator: Navigator
) : BaseViewModel(), LibraryViewModel {
    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    val areFilterGroupExisting: LiveData<Boolean> =
        filtersManager.filterGroups.map { it.isNotEmpty() }

    fun clearFilters() {
        filtersManager.clearFilters()
    }

    fun deleteFilter(filter: be.florien.anyflow.management.filters.model.Filter<*>) {
        filtersManager.removeFilter(filter)
    }

    fun resetFilterChanges() {
        filtersManager.abandonChanges()
    }

    fun getUrlForImage(imageType: String, id: Long): String =
        urlRepository.getArtUrl(imageType, id)
}