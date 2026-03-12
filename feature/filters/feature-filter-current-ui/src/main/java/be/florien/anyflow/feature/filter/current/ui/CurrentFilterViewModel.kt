package be.florien.anyflow.feature.filter.current.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.auth.domain.net.AuthenticationInterceptor
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.Filter
import be.florien.anyflow.urls.UrlRepository
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FilterWithId(
    val id: Int,
    val filter: Filter
)

class CurrentFilterViewModel @Inject constructor(
    override val filtersManager: FiltersManager,
    private val urlRepository: UrlRepository,
    override val navigator: Navigator,
    val authenticationInterceptor: AuthenticationInterceptor
) : BaseViewModel(), LibraryViewModel {
    val stateFlow: StateFlow<PersistentList<FilterWithId>> =
        filtersManager.filtersInEdition.asFlow().map {
            it.mapIndexed { index, params -> FilterWithId(index, params) }.toPersistentList()
        }.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            persistentListOf()
        )
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