package be.florien.anyflow.feature.player.filter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch

open class BaseFilterViewModel(protected val filtersManager: FiltersManager) : BaseViewModel() {

    val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

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