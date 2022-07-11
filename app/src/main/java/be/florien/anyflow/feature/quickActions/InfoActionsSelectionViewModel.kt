package be.florien.anyflow.feature.quickActions

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.feature.player.info.InfoViewModel
import be.florien.anyflow.feature.player.info.SongInfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class InfoActionsSelectionViewModel @Inject constructor(
    context: Context,
    ampache: AmpacheDataSource,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : InfoViewModel(
    context,
    ampache,
    filtersManager,
    orderComposer,
    dataRepository,
    sharedPreferences
) {
    var maxItems = 3
        set(value) {
            field = value
            updateCountDisplay()
        }
    val currentActionsCountDisplay: LiveData<String> =
        MutableLiveData("${songInfoActions.getQuickActions().size}/$maxItems")

    override fun mapActionsRows(initialList: List<SongInfoActions.SongRow>): List<SongInfoActions.SongRow> {
        val mutableList = initialList.toMutableList()
        val quickActions = songInfoActions.getQuickActions()
        quickActions.forEach {
            val indexOfFirst =
                mutableList.indexOfFirst { action -> it.actionType == action.actionType && it.fieldType == action.fieldType }
            if (indexOfFirst >= 0) {
                mutableList[indexOfFirst] = SongInfoActions.SongRow(initialList[indexOfFirst], it.order)
            }
        }
        return mutableList
    }

    override fun executeSongAction(
        fieldType: SongInfoActions.FieldType,
        actionType: SongInfoActions.ActionType
    ) {
        val quickActions = songInfoActions.getQuickActions()
        if (quickActions.size < maxItems || quickActions.any { it.fieldType == fieldType && it.actionType == actionType }) {
            viewModelScope.launch {
                songInfoActions.toggleQuickAction(fieldType, actionType)
                updateRows()
                updateCountDisplay()
            }
        }
    }

    private fun updateCountDisplay() {
        val currentCount = songInfoActions.getQuickActions().size
        currentActionsCountDisplay.mutable.value = "$currentCount/$maxItems"
    }
}