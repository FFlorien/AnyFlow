package be.florien.anyflow.feature.player.ui.library.saved

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.ui.BaseViewModel
import be.florien.anyflow.common.ui.navigation.Navigator
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.model.FilterGroup
import kotlinx.coroutines.launch
import javax.inject.Inject

class SavedFilterGroupViewModel @Inject constructor(
    override val filtersManager: FiltersManager,
    override val navigator: Navigator
) :
    BaseViewModel(),
    LibraryViewModel {
    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)
    val filterGroups: LiveData<List<FilterGroup>> = filtersManager.filterGroups

    fun changeForSavedGroup(savedGroupPosition: Int) {
        viewModelScope.launch {
            val filterGroup = filterGroups.value?.get(savedGroupPosition)
            if (filterGroup != null) {
                filtersManager.loadSavedGroup(filterGroup)
                areFiltersInEdition.mutable.value = false
            }
        }
    }
}