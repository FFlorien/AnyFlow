package be.florien.anyflow.feature.player.filter

import androidx.lifecycle.viewModelScope
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.feature.MutableValueLiveData
import be.florien.anyflow.feature.ValueLiveData
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch

open class BaseFilterViewModel(protected val filtersManager: FiltersManager) : BaseViewModel() {

    val areFiltersInEdition: ValueLiveData<Boolean> = MutableValueLiveData(true)

    fun confirmChanges() {
        viewModelScope.launch {
            filtersManager.commitChanges()
            areFiltersInEdition.mutable.value = false
        }
    }

    fun cancelChanges() {
        filtersManager.abandonChanges()
        areFiltersInEdition.mutable.value = false
    }
}