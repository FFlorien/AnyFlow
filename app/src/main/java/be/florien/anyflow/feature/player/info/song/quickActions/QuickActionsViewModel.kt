package be.florien.anyflow.feature.player.info.song.quickActions

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.song.BaseSongViewModel
import be.florien.anyflow.feature.player.info.song.info.SongInfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class QuickActionsViewModel @Inject constructor(
    context: Context,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : BaseSongViewModel(context, filtersManager, orderComposer, dataRepository, sharedPreferences) {
    override val infoActions = SongInfoActions(
        context.contentResolver, filtersManager, orderComposer, dataRepository, sharedPreferences
    )
    var maxItems = 3
        set(value) {
            field = value
            updateCountDisplay()
        }
    val currentActionsCountDisplay: LiveData<String> =
        MutableLiveData("${infoActions.getQuickActions().size}/$maxItems")

    override fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> {
        val mutableList = initialList.toMutableList()
        val quickActions = infoActions.getQuickActions()
        quickActions.forEach {
            val indexOfFirst =
                mutableList.indexOfFirst { action -> it.actionType == action.actionType && it.fieldType == action.fieldType }
            if (indexOfFirst >= 0) {
                mutableList[indexOfFirst] = InfoActions.InfoRow(initialList[indexOfFirst], it.additionalInfo)
            }
        }
        return mutableList
    }

    override fun executeInfoAction(
        fieldType: InfoActions.FieldType,
        actionType: InfoActions.ActionType
    ) {
        val quickActions = infoActions.getQuickActions()
        if (quickActions.size < maxItems || quickActions.any { it.fieldType == fieldType && it.actionType == actionType }) {
            viewModelScope.launch {
                infoActions.toggleQuickAction(fieldType, actionType)
                updateRows()
                updateCountDisplay()
            }
        }
    }

    private fun updateCountDisplay() {
        val currentCount = infoActions.getQuickActions().size
        currentActionsCountDisplay.mutable.value = "$currentCount/$maxItems"
    }

    override fun getInfoRowList(): MutableList<InfoActions.InfoRow> = infoActions.getInfoRows(song).toMutableList()

    override fun getActionsRows(field: InfoActions.FieldType): List<InfoActions.InfoRow> = infoActions.getActionsRows(song, field)
}