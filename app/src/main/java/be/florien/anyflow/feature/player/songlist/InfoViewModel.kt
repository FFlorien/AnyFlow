package be.florien.anyflow.feature.player.songlist

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.R
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.data.view.SongInfo
import be.florien.anyflow.feature.BaseViewModel
import be.florien.anyflow.player.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class InfoViewModel @Inject constructor(private val filtersManager: FiltersManager, private val orderComposer: OrderComposer, private val dataRepository: DataRepository) : BaseViewModel() {
    internal var connection: PlayerConnection = PlayerConnection()
    private var player: PlayerController = IdlePlayerController()
    var songId = 0L
        set(value) {
            field = value
            initSongInfo()
        }

    val songRows: LiveData<List<SongRow>> = MutableLiveData(listOf())
    val searchTerm: LiveData<String> = MutableLiveData(null)
    val songInfo: LiveData<SongInfo> = MutableLiveData()
    val isPlaylistListDisplayed: LiveData<Boolean> = MutableLiveData(false)

    private fun toggleExpansion(fieldType: FieldType) {
        val mutableList = (songRows.value as List<SongRow>).toMutableList()
        val togglePosition = mutableList.indexOfFirst { it.actionType == ActionType.EXPAND && it.fieldType == fieldType }
        val toggledItem = mutableList.removeAt(togglePosition)
        val isExpanded = mutableList.size > togglePosition && mutableList[togglePosition].actionType != ActionType.EXPAND && mutableList[togglePosition].actionType != ActionType.NONE

        if (isExpanded) {
            val newToggledItem = SongRow(toggledItem.title, toggledItem.text, null, R.drawable.ic_next_occurence, toggledItem.action, toggledItem.fieldType, toggledItem.actionType)
            val filteredList = mutableList.filterNot { it.fieldType == fieldType && it.actionType != ActionType.EXPAND }.toMutableList()
            filteredList.add(togglePosition, newToggledItem)
            (songRows as MutableLiveData).value = filteredList
        } else {
            val newToggledItem = SongRow(toggledItem.title, toggledItem.text, null, R.drawable.ic_previous_occurence, toggledItem.action, toggledItem.fieldType, toggledItem.actionType)
            mutableList.addAll(togglePosition, createOptions(fieldType))
            mutableList.add(togglePosition, newToggledItem)
            (songRows as MutableLiveData).value = mutableList
        }
    }

    private fun createOptions(fieldType: FieldType): List<SongRow> = when (fieldType) {
        FieldType.TITLE -> listOf(
                SongRow(R.string.info_option_next_title, null, R.string.info_option_track_next, R.drawable.ic_add, ::playNext, fieldType, ActionType.ADD_NEXT),
                SongRow(R.string.info_option_add_to_playlist, null, R.string.info_option_add_to_playlist_detail, R.drawable.ic_add_to_playlist, ::displayPlaylistList, fieldType, ActionType.ADD_NEXT),
                SongRow(R.string.info_option_filter_title, songInfo.value?.title, R.string.info_option_filter_on, R.drawable.ic_filter, ::filterOn, fieldType, ActionType.ADD_TO_FILTER),
                SongRow(R.string.info_option_search_title, songInfo.value?.title, R.string.info_option_search_on, R.drawable.ic_search, ::closeAndSearch, fieldType, ActionType.SEARCH),
                SongRow(R.string.info_option_download, null, R.string.info_option_download_description, R.drawable.ic_download, ::download, fieldType, ActionType.DOWNLOAD)
        )
        FieldType.ARTIST -> listOf(
                SongRow(R.string.info_option_filter_title, songInfo.value?.artistName, R.string.info_option_filter_on, R.drawable.ic_filter, ::filterOn, fieldType, ActionType.ADD_TO_FILTER),
                SongRow(R.string.info_option_search_title, songInfo.value?.artistName, R.string.info_option_search_on, R.drawable.ic_search, ::closeAndSearch, fieldType, ActionType.SEARCH)
        )
        FieldType.ALBUM -> listOf(
                SongRow(R.string.info_option_filter_title, songInfo.value?.albumName, R.string.info_option_filter_on, R.drawable.ic_filter, ::filterOn, fieldType, ActionType.ADD_TO_FILTER),
                SongRow(R.string.info_option_search_title, songInfo.value?.albumName, R.string.info_option_search_on, R.drawable.ic_search, ::closeAndSearch, fieldType, ActionType.SEARCH)
        )
        FieldType.ALBUM_ARTIST -> listOf(
                SongRow(R.string.info_option_filter_title, songInfo.value?.albumArtistName, R.string.info_option_filter_on, R.drawable.ic_filter, ::filterOn, fieldType, ActionType.ADD_TO_FILTER),
                SongRow(R.string.info_option_search_title, songInfo.value?.albumArtistName, R.string.info_option_search_on, R.drawable.ic_search, ::closeAndSearch, fieldType, ActionType.SEARCH)
        )
        FieldType.GENRE -> listOf(
                SongRow(R.string.info_option_filter_title, songInfo.value?.genre, R.string.info_option_filter_on, R.drawable.ic_filter, ::filterOn, fieldType, ActionType.ADD_TO_FILTER),
                SongRow(R.string.info_option_search_title, songInfo.value?.genre, R.string.info_option_search_on, R.drawable.ic_search, ::closeAndSearch, fieldType, ActionType.SEARCH)
        )
        else -> listOf()
    }

    private fun playNext(fieldType: FieldType) {
        viewModelScope.launch {
            orderComposer.changeSongPositionForNext(songId)
        }
    }

    private fun displayPlaylistList(fieldType: FieldType) {
        isPlaylistListDisplayed.mutable.value = true
    }

    private fun filterOn(fieldType: FieldType) {
        viewModelScope.launch {
            val songInfo = songInfo.value as SongInfo
            val filter = when (fieldType) {
                FieldType.TITLE -> Filter.SongIs(songInfo.id, songInfo.title, songInfo.art)
                FieldType.ARTIST -> Filter.ArtistIs(songInfo.artistId, songInfo.artistName, null)
                FieldType.ALBUM -> Filter.AlbumIs(songInfo.albumId, songInfo.albumName, songInfo.art)
                FieldType.ALBUM_ARTIST -> Filter.AlbumArtistIs(songInfo.albumArtistId, songInfo.albumArtistName, null)
                FieldType.GENRE -> Filter.GenreIs(songInfo.genre)
                else -> throw IllegalArgumentException("This field can't be filtered on")
            }
            filtersManager.clearFilters()
            filtersManager.addFilter(filter)
            filtersManager.commitChanges()
        }
    }

    private fun closeAndSearch(fieldType: FieldType) {
        val songInfo = songInfo.value as SongInfo
        when (fieldType) {
            FieldType.TITLE -> (searchTerm as MutableLiveData).value = songInfo.title
            FieldType.ARTIST -> (searchTerm as MutableLiveData).value = songInfo.artistName
            FieldType.ALBUM -> (searchTerm as MutableLiveData).value = songInfo.albumName
            FieldType.ALBUM_ARTIST -> (searchTerm as MutableLiveData).value = songInfo.albumArtistName
            FieldType.GENRE -> (searchTerm as MutableLiveData).value = songInfo.genre
            else -> throw IllegalArgumentException("This field can't be searched on")
        }
    }

    private fun download(fieldType: FieldType) {
        viewModelScope.launch {
            player.download(songId)
        }
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
                SongRow(R.string.info_title, value.title, null, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.TITLE, ActionType.EXPAND),
                SongRow(R.string.info_artist, value.artistName, null, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.ARTIST, ActionType.EXPAND),
                SongRow(R.string.info_track, value.track.toString(), null, 0, null, FieldType.TRACK, ActionType.NONE),
                SongRow(R.string.info_album, value.albumName, null, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.ALBUM, ActionType.EXPAND),
                SongRow(R.string.info_album_artist, value.albumArtistName, null, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.ALBUM_ARTIST, ActionType.EXPAND),
                SongRow(R.string.info_genre, value.genre, null, R.drawable.ic_next_occurence, ::toggleExpansion, FieldType.GENRE, ActionType.EXPAND),
                SongRow(R.string.info_duration, value.timeText, null, 0, null, FieldType.DURATION, ActionType.NONE),
                SongRow(R.string.info_year, value.year.toString(), null, 0, null, FieldType.YEAR, ActionType.NONE)
        )
    }

    class SongRow(@StringRes val title: Int, val text: String?, @StringRes val textRes: Int?, @DrawableRes val icon: Int, val action: ((FieldType) -> Unit)?, val fieldType: FieldType, val actionType: ActionType)

    enum class FieldType {
        TITLE, TRACK, ARTIST, ALBUM, ALBUM_ARTIST, GENRE, YEAR, DURATION
    }

    enum class ActionType {
        NONE, EXPAND, ADD_TO_FILTER, ADD_NEXT, SEARCH, DOWNLOAD
    }

    inner class PlayerConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            player = (service as PlayerService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            player = IdlePlayerController()
        }
    }
}