package be.florien.anyflow.feature.quickOptions

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.feature.player.songlist.InfoViewModel
import be.florien.anyflow.feature.player.songlist.SongInfoOptions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class InfoOptionsSelectionViewModel @Inject constructor(
    context: Context,
    ampache: AmpacheConnection,
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
    val currentOptionsCountDisplay: LiveData<String> =
        MutableLiveData("${songInfoOptions.getQuickOptions().size}/$maxItems")

    override fun mapOptionsRows(initialList: List<SongInfoOptions.SongRow>): List<SongInfoOptions.SongRow> {
        val mutableList = initialList.toMutableList()
        val quickOptions = songInfoOptions.getQuickOptions()
        quickOptions.forEach {
            val indexOfFirst =
                mutableList.indexOfFirst { option -> it.actionType == option.actionType && it.fieldType == option.fieldType }
            if (indexOfFirst >= 0) {
                mutableList[indexOfFirst] = SongInfoOptions.SongRow(initialList[indexOfFirst], it.order)
            }
        }
        return mutableList
    }

    override fun executeSongAction(
        fieldType: SongInfoOptions.FieldType,
        actionType: SongInfoOptions.ActionType
    ) {
        val quickOptions = songInfoOptions.getQuickOptions()
        if (quickOptions.size < maxItems || quickOptions.any { it.fieldType == fieldType && it.actionType == actionType }) {
            viewModelScope.launch {
                songInfoOptions.toggleQuickOption(fieldType, actionType)
                updateRows()
                updateCountDisplay()
            }
        }
    }

    private fun updateCountDisplay() {
        val currentCount = songInfoOptions.getQuickOptions().size
        currentOptionsCountDisplay.mutable.value = "$currentCount/$maxItems"
    }
}