package be.florien.anyflow.feature.player.ui.info.song.shortcuts

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.download.DownloadManager
import be.florien.anyflow.feature.player.services.queue.FiltersManager
import be.florien.anyflow.feature.player.services.queue.OrderComposer
import be.florien.anyflow.feature.player.ui.info.InfoActions
import be.florien.anyflow.feature.player.ui.info.song.BaseSongViewModel
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import kotlinx.coroutines.launch
import javax.inject.Inject

class ShortcutsViewModel @Inject constructor(
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
        MutableLiveData("${infoActions.getShortcuts().size}/$maxItems")
    var dummySongInfo: SongInfo
        get() = songInfoMediator.value ?: SongInfo.dummySongInfo(SongInfoActions.DUMMY_SONG_ID)
        set(value) {
            songInfoMediator.value = value
        }

    override fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> {
        val mutableList = initialList.toMutableList()
        val shortcuts = infoActions.getShortcuts()
        shortcuts.forEach {
            val indexOfFirst =
                mutableList.indexOfFirst { action -> it.actionType == action.actionType && it.fieldType == action.fieldType }
            if (indexOfFirst >= 0) {
                mutableList[indexOfFirst] =
                    SongInfoActions.ShortcutInfoRow(initialList[indexOfFirst], it.order)
            }
        }
        return mutableList
    }

    override fun executeAction(row: InfoActions.InfoRow): Boolean {
        if (super.executeAction(row)) {
            return true
        }
        val shortcuts = infoActions.getShortcuts()
        if (shortcuts.size < maxItems || shortcuts.any { it.fieldType == row.fieldType && it.actionType == row.actionType }) {
            viewModelScope.launch {
                infoActions.toggleShortcut(row.fieldType, row.actionType)
                updateRows()
                updateCountDisplay()
            }
            return true
        }
        return false
    }

    private fun updateCountDisplay() {
        val currentCount = infoActions.getShortcuts().size
        currentActionsCountDisplay.mutable.value = "$currentCount/$maxItems"
    }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(songInfo).toMutableList()

    override suspend fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(songInfo, row)
}