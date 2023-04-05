package be.florien.anyflow.feature.player.library.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.library.LibraryViewModel
import be.florien.anyflow.player.FiltersManager
import javax.inject.Inject

class DisplayFilterViewModel @Inject constructor(
    override val filtersManager: FiltersManager,
    val urlRepository: UrlRepository
) : BaseViewModel(), LibraryViewModel {
    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    val areFilterGroupExisting: LiveData<Boolean> =
        filtersManager.filterGroups.map { it.isNotEmpty() }

    fun clearFilters() {
        filtersManager.clearFilters()
    }

    fun deleteFilter(filter: Filter<*>) {
        filtersManager.removeFilter(filter)
    }

    fun resetFilterChanges() {
        filtersManager.abandonChanges()
    }

    fun getUrlForImage(imageType: String, id: Long): String =
        urlRepository.getArtUrl(imageType, id)
}