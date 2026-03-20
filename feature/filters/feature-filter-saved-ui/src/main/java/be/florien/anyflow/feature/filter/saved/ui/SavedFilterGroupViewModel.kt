package be.florien.anyflow.feature.filter.saved.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.base.BaseViewModel
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.utils.TimeOperations
import be.florien.anyflow.feature.library.ui.LibraryViewModel
import be.florien.anyflow.management.filters.FiltersManager
import be.florien.anyflow.management.filters.domain.model.FilterGroup
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FilterGroupItem(
    val id: Long,
    val name: String,
    val filtersDescription: String
)

class SavedFilterGroupViewModel @Inject constructor(
    override val filtersManager: FiltersManager,
    override val navigator: Navigator
) : BaseViewModel(), LibraryViewModel {
    val state = filtersManager.filterGroups.map { groups ->
        groups
            .mapNotNull {
                when (it) {
                    is FilterGroup.SavedFilterGroup -> FilterGroupItem(
                        it.id,
                        it.name,
                        it.filters.joinToString { it.getFullDisplay() })

                    is FilterGroup.HistoryFilterGroup -> FilterGroupItem(
                        it.id,
                        TimeOperations.toDisplayDateTime(it.dateAdded.timeInMillis),
                        it.filters.joinToString { it.getFullDisplay() })

                    is FilterGroup.CurrentFilterGroup -> null
                }
            }
            .toPersistentList()
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        persistentListOf()
    )
    override val areFiltersInEdition: LiveData<Boolean> = MutableLiveData(true)

    fun changeForSavedGroup(savedGroupId: Long) {
        viewModelScope.launch {
            filtersManager.loadSavedGroup(savedGroupId)
            areFiltersInEdition.mutable.value = false
        }
    }
}