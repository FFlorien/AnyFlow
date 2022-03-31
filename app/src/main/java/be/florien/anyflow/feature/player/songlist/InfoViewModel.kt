package be.florien.anyflow.feature.player.songlist

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
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
    val songRows: LiveData<List<SongInfoOptions.SongRow>> = MutableLiveData(listOf())
    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<Boolean> = MutableLiveData(false)
    private val expandedSections = mutableListOf<SongInfoOptions.FieldType>()
    private val contentResolver = context.contentResolver
    protected var songInfoOptions = SongInfoOptions(contentResolver, ampache, filtersManager, orderComposer, dataRepository, sharedPreferences)
    var song: Song = Song(0, "","","","",0,"", "", "")
        set(value) {
            field = value
            viewModelScope.launch {
                songInfo.mutable.value = songInfoOptions.getSongInfo(song)
                updateRows()
            }
        }

    /**
     * Public methods
     */

    open fun executeAction(fieldType: SongInfoOptions.FieldType, actionType: SongInfoOptions.ActionType) {
        viewModelScope.launch {
            when (actionType) {
                SongInfoOptions.ActionType.EXPAND_TITLE -> toggleExpansion(fieldType)
                else -> executeSongAction(fieldType, actionType)
            }
        }
    }

    protected suspend fun updateRows() {
        val mutableList = songInfoOptions.getInfoRows(song.id).toMutableList()
        for (fieldType in expandedSections) {
            val togglePosition = mutableList.indexOfFirst { it.actionType == SongInfoOptions.ActionType.EXPAND_TITLE && it.fieldType == fieldType }
            val toggledItem = mutableList.removeAt(togglePosition)
            val newToggledItem = SongInfoOptions.SongRow(toggledItem.title, toggledItem.text, null, R.drawable.ic_previous_occurence, toggledItem.fieldType, toggledItem.actionType)
            mutableList.addAll(togglePosition, songInfoOptions.getOptionsRows(song.id, fieldType))
            mutableList.add(togglePosition, newToggledItem)
        }
        songRows.mutable.value = mapOptionsRows(mutableList)
    }

    abstract fun executeSongAction(fieldType: SongInfoOptions.FieldType, actionType: SongInfoOptions.ActionType)

    abstract fun mapOptionsRows(initialList: List<SongInfoOptions.SongRow>): List<SongInfoOptions.SongRow>

    /**
     * Private methods: actions
     */

    private suspend fun toggleExpansion(fieldType: SongInfoOptions.FieldType) {
        if (!expandedSections.remove(fieldType)) {
            expandedSections.add(fieldType)
        }
        updateRows()
    }
}