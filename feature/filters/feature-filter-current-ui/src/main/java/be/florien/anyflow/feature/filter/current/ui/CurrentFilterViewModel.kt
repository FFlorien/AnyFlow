package be.florien.anyflow.feature.filter.current.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.urls.UrlRepository
import javax.inject.Inject

class CurrentFilterViewModel @Inject constructor(
    override val filtersManager: FiltersManager,
    private val urlRepository: UrlRepository,
    override val navigator: Navigator
) : BaseViewModel(), LibraryViewModel {
    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    fun clearFilters() {
        filtersManager.clearFilters()
    }

    fun deleteFilter(filter: Filter) {
        filtersManager.removeFilter(filter)
    }

    fun getUrlForImage(imageType: String, id: Long): String =
        urlRepository.getArtUrl(imageType, id)
}