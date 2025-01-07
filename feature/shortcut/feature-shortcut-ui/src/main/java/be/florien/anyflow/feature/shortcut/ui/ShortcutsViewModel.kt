package be.florien.anyflow.feature.shortcut.ui

import android.content.SharedPreferences
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.feature.song.base.domain.BaseSongInfoActions.Companion.DUMMY_SONG_ID
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.ui.BaseSongViewModel
import be.florien.anyflow.management.filters.model.FilterType
import be.florien.anyflow.management.queue.model.SongDisplay
import be.florien.anyflow.tags.local.model.DownloadProgressState
import be.florien.anyflow.tags.model.SongInfo
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class ShortcutsViewModel @Inject constructor(
    @Named("preferences") sharedPreferences: SharedPreferences
) : BaseSongViewModel() {

     private val infoActions: ShortcutSongInfoActions = ShortcutSongInfoActions(sharedPreferences)

    override var songId: Long = DUMMY_SONG_ID
    var maxItems = 3
        set(value) {
            field = value
            updateCountDisplay()
        }
    val currentActionsCountDisplay: LiveData<String> =
        MutableLiveData("${infoActions.getShortcuts().size}/$maxItems")
    var dummySongInfo: SongInfo
        get() = songInfoMediator.value ?: SongInfo.dummySongInfo(DUMMY_SONG_ID)
        set(value) {
            songInfoMediator.value = value
        }
    var dummySongDisplay = SongDisplay(
        DUMMY_SONG_ID,
        "",
        "",
        "",
        0L,
        0
    )
    val dummyCover = ImageConfig(null, R.drawable.cover_placeholder, View.VISIBLE)
    val shortcutsList: List<BaseSongInfoRow.ShortcutInfoRow>
        get() = infoActions.getShortcuts()

    override fun mapActionsRows(initialList: List<BaseSongInfoRow>): List<BaseSongInfoRow> {
        val mutableList = initialList.toMutableList()
        val shortcuts = infoActions.getShortcuts()
        shortcuts.forEach {
            val indexOfFirst =
                mutableList.indexOfFirst { action -> it.actionType == action.actionType && it.fieldType == action.fieldType }
            if (indexOfFirst >= 0) {
                mutableList[indexOfFirst] =
                    BaseSongInfoRow.ShortcutInfoRow(initialList[indexOfFirst], it.order)
            }
        }
        return mutableList
    }

    override fun executeAction(row: BaseSongInfoRow): Boolean {
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


    override fun getDownloadState(
        id: Long,
        type: FilterType,
        additionalInfo: Int?
    ): LiveData<DownloadProgressState> = MutableLiveData(DownloadProgressState(100, 0, 0))
}