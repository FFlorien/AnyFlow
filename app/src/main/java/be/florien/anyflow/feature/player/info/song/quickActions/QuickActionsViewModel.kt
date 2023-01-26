package be.florien.anyflow.feature.player.info.song.quickActions

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.song.BaseSongViewModel
import be.florien.anyflow.feature.player.info.song.SongInfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class QuickActionsViewModel @Inject constructor(
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences,
    downloadManager: DownloadManager
) : BaseSongViewModel(filtersManager, orderComposer, dataRepository, sharedPreferences, downloadManager) {
    override val infoActions = SongInfoActions(
        filtersManager, orderComposer, dataRepository, sharedPreferences, downloadManager
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
                mutableList[indexOfFirst] = SongInfoActions.QuickActionInfoRow(initialList[indexOfFirst], it.order)
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

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> = infoActions.getInfoRows(song).toMutableList()

    override suspend fun getActionsRows(field: InfoActions.FieldType): List<InfoActions.InfoRow> = infoActions.getActionsRows(song, field)
}