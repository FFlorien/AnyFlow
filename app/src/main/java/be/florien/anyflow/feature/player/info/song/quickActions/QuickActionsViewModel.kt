package be.florien.anyflow.feature.player.info.song.quickActions

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.view.SongInfo
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
    urlRepository: UrlRepository,
    sharedPreferences: SharedPreferences,
    downloadManager: DownloadManager
) : BaseSongViewModel(
    filtersManager,
    orderComposer,
    dataRepository,
    urlRepository,
    sharedPreferences,
    downloadManager
) {
    var maxItems = 3
        set(value) {
            field = value
            updateCountDisplay()
        }
    val currentActionsCountDisplay: LiveData<String> =
        MutableLiveData("${infoActions.getQuickActions().size}/$maxItems")
    var dummySongInfo: SongInfo
        get() = songInfoMediator.value ?: SongInfo.dummySongInfo(SongInfoActions.DUMMY_SONG_ID)
        set(value) {
            songInfoMediator.value = value
        }

    override fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> {
        val mutableList = initialList.toMutableList()
        val quickActions = infoActions.getQuickActions()
        quickActions.forEach {
            val indexOfFirst =
                mutableList.indexOfFirst { action -> it.actionType == action.actionType && it.fieldType == action.fieldType }
            if (indexOfFirst >= 0) {
                mutableList[indexOfFirst] =
                    SongInfoActions.QuickActionInfoRow(initialList[indexOfFirst], it.order)
            }
        }
        return mutableList
    }

    override fun executeAction(row: InfoActions.InfoRow): Boolean {
        if (super.executeAction(row)) {
            return true
        }
        val quickActions = infoActions.getQuickActions()
        if (quickActions.size < maxItems || quickActions.any { it.fieldType == row.fieldType && it.actionType == row.actionType }) {
            viewModelScope.launch {
                infoActions.toggleQuickAction(row.fieldType, row.actionType)
                updateRows()
                updateCountDisplay()
            }
            return true
        }
        return false
    }

    private fun updateCountDisplay() {
        val currentCount = infoActions.getQuickActions().size
        currentActionsCountDisplay.mutable.value = "$currentCount/$maxItems"
    }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(songInfo).toMutableList()

    override suspend fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(songInfo, row)
}