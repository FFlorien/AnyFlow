package be.florien.anyflow.feature.player.songlist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.player.FiltersManager
import kotlinx.coroutines.launch
import javax.inject.Inject

class InfoViewModel @Inject constructor(private val filtersManager: FiltersManager, private val dataRepository: DataRepository) : BaseViewModel() {
    var songId = 0L
        set(value) {
            field = value
            initSongInfo()
        }

    val songRows: LiveData<List<SongRow>> = MutableLiveData(listOf())

    val songInfo: LiveData<SongInfo> = MutableLiveData()

    private fun toggleExpansion(fieldType: FieldType, actionType: ActionType) {
        val mutableList = (songRows.value as List<SongRow>).toMutableList()
        val togglePosition = mutableList.indexOfFirst { it.actionType == ActionType.EXPAND && it.fieldType == fieldType }
        val toggledItem = mutableList.removeAt(togglePosition)
        val isExpanded = mutableList.size > togglePosition && mutableList[togglePosition].actionType != ActionType.EXPAND && mutableList[togglePosition].actionType != ActionType.NONE

        if (isExpanded) {
            val newToggledItem = SongRow(toggledItem.title, toggledItem.text, R.drawable.ic_next_occurence, toggledItem.action, toggledItem.fieldType, toggledItem.actionType)
            val filteredList = mutableList.filterNot { it.fieldType == fieldType && it.actionType != ActionType.EXPAND }.toMutableList()
            filteredList.add(togglePosition, newToggledItem)
            (songRows as MutableLiveData).value = filteredList
        } else {
            val newToggledItem = SongRow(toggledItem.title, toggledItem.text, R.drawable.ic_previous_occurence, toggledItem.action, toggledItem.fieldType, toggledItem.actionType)
            mutableList.addAll(togglePosition, createOptions(fieldType))
            mutableList.add(togglePosition, newToggledItem)
            (songRows as MutableLiveData).value = mutableList
        }
    }

    private fun createOptions(fieldType: FieldType): List<SongRow> = listOf(
            SongRow(R.string.action_filter_add, "Todo", R.drawable.ic_add, ::makeActionTodo, fieldType, ActionType.ADD_TO_FILTER)
    )

    private fun makeActionTodo(fieldType: FieldType, actionType: ActionType) {
        //TODO("Not yet implemented")
    }

    private fun initSongInfo() {
        viewModelScope.launch {
            (songInfo as MutableLiveData).value = dataRepository.getSongById(songId) ?: throw IllegalArgumentException("No song for ID")
            initList()
        }
    }

    private fun initList() {
        val value = songInfo.value
        if (value == null) {
            (songRows as MutableLiveData).value = listOf()
            return
        }
        (songRows as MutableLiveData).value = listOf(
                SongRow(R.string.info_title, value.title, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.SONG, ActionType.EXPAND),
                SongRow(R.string.info_duration, value.timeText, 0, null, FieldType.DURATION, ActionType.NONE),
                SongRow(R.string.info_artist, value.artistName, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.ARTIST, ActionType.EXPAND),
                SongRow(R.string.info_track, value.track.toString(), 0, null, FieldType.TRACK, ActionType.NONE),
                SongRow(R.string.info_album, value.albumName, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.ALBUM, ActionType.EXPAND),
                SongRow(R.string.info_year, value.year.toString(), 0, null, FieldType.YEAR, ActionType.NONE),
                SongRow(R.string.info_album_artist, value.albumArtistName, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.ALBUM_ARTIST, ActionType.EXPAND),
                SongRow(R.string.info_genre, value.genre, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.GENRE, ActionType.EXPAND)
        )
    }

    class SongRow(@StringRes val title: Int, val text: String, @DrawableRes val icon: Int, val action: ((FieldType, ActionType) -> Unit)?, val fieldType: FieldType, val actionType: ActionType)

    enum class FieldType {
        SONG, TRACK, ARTIST, ALBUM, ALBUM_ARTIST, GENRE, YEAR, DURATION
    }

    enum class ActionType {
        NONE, EXPAND, ADD_TO_FILTER
    }
}