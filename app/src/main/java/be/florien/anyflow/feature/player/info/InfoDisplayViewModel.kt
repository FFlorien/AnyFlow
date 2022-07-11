package be.florien.anyflow.feature.player.info

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheDataSource
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class InfoDisplayViewModel @Inject constructor(
    context: Context,
    ampache: AmpacheDataSource,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : InfoViewModel(
    context,
    ampache,
    filtersManager,
    orderComposer,
    dataRepository,
    sharedPreferences
) {

    override fun executeSongAction(
        fieldType: SongInfoActions.FieldType,
        actionType: SongInfoActions.ActionType
    ) {
        viewModelScope.launch {
            when (actionType) {
                SongInfoActions.ActionType.ADD_NEXT -> songInfoActions.playNext(song.id)
                SongInfoActions.ActionType.ADD_TO_PLAYLIST -> displayPlaylistList()
                SongInfoActions.ActionType.ADD_TO_FILTER -> songInfoActions.filterOn(
                    song,
                    fieldType
                )
                SongInfoActions.ActionType.SEARCH -> searchTerm.mutable.value =
                    songInfoActions.getSearchTerms(song, fieldType)
                SongInfoActions.ActionType.DOWNLOAD -> songInfoActions.download(song)
                SongInfoActions.ActionType.NONE, SongInfoActions.ActionType.INFO_TITLE, SongInfoActions.ActionType.EXPANDABLE_TITLE, SongInfoActions.ActionType.EXPANDED_TITLE -> return@launch
            }
        }
    }

    override fun mapActionsRows(initialList: List<SongInfoActions.SongRow>): List<SongInfoActions.SongRow> =
        initialList

    private fun displayPlaylistList() {
        isPlaylistListDisplayed.mutable.value = true
    }
}