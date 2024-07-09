package be.florien.anyflow.feature.player.ui.info.song.shortcuts

import android.content.SharedPreferences
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.tags.model.SongInfo
import be.florien.anyflow.tags.view.SongDisplay
import be.florien.anyflow.feature.download.DownloadManager
import be.florien.anyflow.feature.player.services.queue.OrderComposer
import be.florien.anyflow.feature.player.ui.info.song.BaseSongViewModel
import be.florien.anyflow.feature.player.ui.info.song.SongInfoActions
import be.florien.anyflow.management.filters.FiltersManager
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class ShortcutsViewModel @Inject constructor(
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: be.florien.anyflow.tags.DataRepository,
    urlRepository: be.florien.anyflow.tags.UrlRepository,
    @Named("preferences") sharedPreferences: SharedPreferences,
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
    var dummySongDisplay = SongDisplay(
        SongInfoActions.DUMMY_SONG_ID,
        "",
        "",
        "",
        0L,
        0
    )
    val dummyCover = ImageConfig(null, R.drawable.cover_placeholder, View.VISIBLE)
    val shortcutsList: List<SongInfoActions.ShortcutInfoRow>
        get() = infoActions.getShortcuts()

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