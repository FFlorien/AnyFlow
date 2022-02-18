package be.florien.anyflow.feature.player.songlist

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class InfoViewModel @Inject constructor(
    context: Context,
    private val ampache: AmpacheConnection,
    private val filtersManager: FiltersManager,
    private val orderComposer: OrderComposer,
    private val dataRepository: DataRepository
) : BaseViewModel() {
    val songInfo: LiveData<SongInfo> = MutableLiveData()
    val songRows: LiveData<List<SongInfoOptions.SongRow>> = MutableLiveData(listOf())
    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<Boolean> = MutableLiveData(false)
    private val contentResolver = context.contentResolver
    private lateinit var songInfoOptions: SongInfoOptions

    /**
     * Public methods
     */

    fun setSongId(songId: Long) {
        songInfoOptions = SongInfoOptions(songId, contentResolver, ampache, filtersManager, orderComposer, dataRepository)
        viewModelScope.launch {
            songInfoOptions.initSongInfo()
            songInfo.mutable.value = songInfoOptions.songInfo
            songRows.mutable.value = songInfoOptions.getInfoRows()
        }
    }

    fun executeAction(fieldType: SongInfoOptions.FieldType, actionType: SongInfoOptions.ActionType) {
        viewModelScope.launch {
            when (actionType) {
                SongInfoOptions.ActionType.EXPAND_TITLE -> toggleExpansion(fieldType)
                SongInfoOptions.ActionType.ADD_NEXT -> songInfoOptions.playNext()
                SongInfoOptions.ActionType.ADD_TO_PLAYLIST -> displayPlaylistList()
                SongInfoOptions.ActionType.ADD_TO_FILTER -> songInfoOptions.filterOn(fieldType)
                SongInfoOptions.ActionType.SEARCH -> searchTerm.mutable.value = songInfoOptions.getSearchTerms(fieldType)
                SongInfoOptions.ActionType.DOWNLOAD -> songInfoOptions.download()
                SongInfoOptions.ActionType.NONE, SongInfoOptions.ActionType.INFO_TITLE -> return@launch
            }
        }
    }

    /**
     * Private methods: actions
     */

    private fun toggleExpansion(fieldType: SongInfoOptions.FieldType) {
        val mutableList = (songRows.value as List<SongInfoOptions.SongRow>).toMutableList()
        val togglePosition = mutableList.indexOfFirst { it.actionType == SongInfoOptions.ActionType.EXPAND_TITLE && it.fieldType == fieldType }
        val toggledItem = mutableList.removeAt(togglePosition)
        val isExpanded = mutableList.size > togglePosition && mutableList[togglePosition].actionType != SongInfoOptions.ActionType.EXPAND_TITLE && mutableList[togglePosition].actionType != SongInfoOptions.ActionType.INFO_TITLE

        if (isExpanded) {
            val newToggledItem = SongInfoOptions.SongRow(toggledItem.title, toggledItem.text, null, R.drawable.ic_next_occurence, toggledItem.fieldType, toggledItem.actionType)
            val filteredList = mutableList.filterNot { it.fieldType == fieldType && it.actionType != SongInfoOptions.ActionType.EXPAND_TITLE }.toMutableList()
            filteredList.add(togglePosition, newToggledItem)
            (songRows as MutableLiveData).value = filteredList
        } else {
            val newToggledItem = SongInfoOptions.SongRow(toggledItem.title, toggledItem.text, null, R.drawable.ic_previous_occurence, toggledItem.fieldType, toggledItem.actionType)
            mutableList.addAll(togglePosition, songInfoOptions.getOptionsRows(fieldType))
            mutableList.add(togglePosition, newToggledItem)
            (songRows as MutableLiveData).value = mutableList
        }
    }

    private fun displayPlaylistList() {
        isPlaylistListDisplayed.mutable.value = true
    }
}