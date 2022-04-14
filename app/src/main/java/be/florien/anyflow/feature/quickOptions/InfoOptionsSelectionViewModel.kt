package be.florien.anyflow.feature.quickOptions

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.server.AmpacheConnection
import be.florien.anyflow.feature.player.songlist.InfoViewModel
import be.florien.anyflow.feature.player.songlist.SongInfoOptions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class InfoOptionsSelectionViewModel @Inject constructor(
    context: Context,
    ampache: AmpacheConnection,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : InfoViewModel(context, ampache, filtersManager, orderComposer, dataRepository, sharedPreferences) {
    val shouldUpdateSongList: LiveData<Boolean> = MutableLiveData(false)

    override fun mapOptionsRows(initialList: List<SongInfoOptions.SongRow>): List<SongInfoOptions.SongRow> {
        val mutableList = initialList.toMutableList()
        val quickOptions = songInfoOptions.getQuickOptions()
        quickOptions.forEach {
            val indexOfFirst = mutableList.indexOfFirst { option -> it.actionType == option.actionType && it.fieldType == option.fieldType }
            if (indexOfFirst >= 0) {
                mutableList[indexOfFirst] = it
            }
        }
        return mutableList
    }

    override fun executeSongAction(fieldType: SongInfoOptions.FieldType, actionType: SongInfoOptions.ActionType) {
        viewModelScope.launch {
            songInfoOptions.toggleQuickOption(fieldType, actionType)
            updateRows()
            shouldUpdateSongList.mutable.value = true
        }
    }
}