package be.florien.anyflow.feature.library.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.info.InfoViewModel
import be.florien.anyflow.feature.library.tags.domain.model.IdText
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter

abstract class LibraryInfoViewModel<T>(
    override val filtersManager: FiltersManager,
    override val navigator: Navigator
) : InfoViewModel<T>(), LibraryViewModel {

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    var filterNavigation: Filter<*>? = null
        set(value) {
            field = value
            updateRows()
        }

    override fun executeAction(row: T) = true

    abstract fun getArtUrl(artType: String, id: Long): String?

    abstract suspend fun getFilteredInfo(filterType: Filter.FilterType, filter: Filter<*>?): IdText?
}