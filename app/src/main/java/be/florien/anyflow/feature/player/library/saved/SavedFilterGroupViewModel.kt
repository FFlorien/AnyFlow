package be.florien.anyflow.feature.player.library.saved

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.view.FilterGroup
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.player.library.LibraryViewModel
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class SavedFilterGroupViewModel @Inject constructor(override val filtersManager: FiltersManager) : BaseViewModel(), LibraryViewModel {
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