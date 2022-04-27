package be.florien.anyflow.feature.player.songlist

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.view.Song
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch

abstract class InfoViewModel(
    context: Context,
    ampache: AmpacheConnection,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : BaseViewModel() {
    val songInfo: LiveData<SongInfo> = MutableLiveData()
    val songRows: LiveData<List<SongInfoActions.SongRow>> = MutableLiveData(listOf())
    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<Boolean> = MutableLiveData(false)
    private val expandedSections = mutableListOf<SongInfoActions.FieldType>()
    private val contentResolver = context.contentResolver
    protected var songInfoActions = SongInfoActions(contentResolver, ampache, filtersManager, orderComposer, dataRepository, sharedPreferences)
    var song: Song = Song(0, "","","","",0,"", "", "")
        set(value) {
            field = value
            viewModelScope.launch {
                songInfo.mutable.value = songInfoActions.getSongInfo(song)
                updateRows()
            }
        }

    /**
     * Public methods
     */

    open fun executeAction(fieldType: SongInfoActions.FieldType, actionType: SongInfoActions.ActionType) {
        viewModelScope.launch {
            when (actionType) {
                SongInfoActions.ActionType.EXPANDABLE_TITLE, SongInfoActions.ActionType.EXPANDED_TITLE -> toggleExpansion(fieldType)
                else -> executeSongAction(fieldType, actionType)
            }
        }
    }

    protected suspend fun updateRows() {
        val mutableList = songInfoActions.getInfoRows(song.id).toMutableList()
        for (fieldType in expandedSections) {
            val togglePosition = mutableList.indexOfFirst { it.actionType == SongInfoActions.ActionType.EXPANDABLE_TITLE && it.fieldType == fieldType }
            val toggledItem = mutableList.removeAt(togglePosition)
            val newToggledItem = SongInfoActions.SongRow(toggledItem.title, toggledItem.text, null, toggledItem.fieldType, SongInfoActions.ActionType.EXPANDED_TITLE)
            mutableList.addAll(togglePosition, songInfoActions.getActionsRows(song.id, fieldType))
            mutableList.add(togglePosition, newToggledItem)
        }
        songRows.mutable.value = mapActionsRows(mutableList)
    }

    abstract fun executeSongAction(fieldType: SongInfoActions.FieldType, actionType: SongInfoActions.ActionType)

    abstract fun mapActionsRows(initialList: List<SongInfoActions.SongRow>): List<SongInfoActions.SongRow>

    /**
     * Private methods: actions
     */

    private suspend fun toggleExpansion(fieldType: SongInfoActions.FieldType) {
        if (!expandedSections.remove(fieldType)) {
            expandedSections.add(fieldType)
        }
        updateRows()
    }
}