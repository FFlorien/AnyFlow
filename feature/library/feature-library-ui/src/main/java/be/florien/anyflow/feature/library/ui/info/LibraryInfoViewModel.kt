package be.florien.anyflow.feature.library.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.common.ui.info.InfoViewModel
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.Filter

abstract class LibraryInfoViewModel<IA: InfoActions<Filter<*>?>>(
    override val filtersManager: FiltersManager,
    override val navigator: be.florien.anyflow.common.navigation.Navigator
) : InfoViewModel<Filter<*>?, IA>(), LibraryViewModel {

    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    var filterNavigation: Filter<*>? = null
        set(value) {
            field = value
            updateRows()
        }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(filterNavigation).toMutableList()

    override suspend fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(filterNavigation, row)

    override fun executeAction(row: InfoActions.InfoRow) = true
}