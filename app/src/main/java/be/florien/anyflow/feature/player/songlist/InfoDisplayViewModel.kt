package be.florien.anyflow.feature.player.songlist

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class InfoDisplayViewModel @Inject constructor(
    context: Context,
    ampache: AmpacheConnection,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : InfoViewModel(context, ampache, filtersManager, orderComposer, dataRepository, sharedPreferences) {

    override fun executeSongAction(fieldType: SongInfoOptions.FieldType, actionType: SongInfoOptions.ActionType) {
        viewModelScope.launch {
            when (actionType) {
                SongInfoOptions.ActionType.ADD_NEXT -> songInfoOptions.playNext(song.id)
                SongInfoOptions.ActionType.ADD_TO_PLAYLIST -> displayPlaylistList()
                SongInfoOptions.ActionType.ADD_TO_FILTER -> songInfoOptions.filterOn(song.id, fieldType)
                SongInfoOptions.ActionType.SEARCH -> searchTerm.mutable.value = songInfoOptions.getSearchTerms(song.id, fieldType)
                SongInfoOptions.ActionType.DOWNLOAD -> songInfoOptions.download(song.id)
                SongInfoOptions.ActionType.NONE, SongInfoOptions.ActionType.INFO_TITLE, SongInfoOptions.ActionType.EXPANDABLE_TITLE -> return@launch
            }
        }
    }

    override fun mapOptionsRows(initialList: List<SongInfoOptions.SongRow>): List<SongInfoOptions.SongRow> = initialList

    private fun displayPlaylistList() {
        isPlaylistListDisplayed.mutable.value = true
    }
}