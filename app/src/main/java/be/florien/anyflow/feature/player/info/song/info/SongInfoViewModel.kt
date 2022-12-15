package be.florien.anyflow.feature.player.info.song.info

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.feature.player.info.song.BaseSongViewModel
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongInfoViewModel @Inject constructor(
    context: Context,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : BaseSongViewModel(context, filtersManager, orderComposer, dataRepository, sharedPreferences) {
    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<Boolean> = MutableLiveData(false)
    override val infoActions = SongInfoActions(
        context.contentResolver,
        filtersManager,
        orderComposer,
        dataRepository,
        sharedPreferences
    )


    override fun executeInfoAction(
        fieldType: InfoActions.FieldType,
        actionType: InfoActions.ActionType
    ) {
        if (fieldType !is InfoActions.SongFieldType || actionType !is InfoActions.SongActionType) {
            return
        }

        viewModelScope.launch {
            when (actionType) {
                is InfoActions.SongActionType.AddNext -> infoActions.playNext(song.id)
                is InfoActions.SongActionType.AddToPlaylist -> displayPlaylistList()
                is InfoActions.SongActionType.AddToFilter -> infoActions.filterOn(
                    song,
                    fieldType
                )
                is InfoActions.SongActionType.Search -> searchTerm.mutable.value =
                    infoActions.getSearchTerms(song, fieldType)
                is InfoActions.SongActionType.Download -> infoActions.download(song)
                else -> return@launch
            }
        }
    }

    override fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> =
        initialList

    private fun displayPlaylistList() {
        isPlaylistListDisplayed.mutable.value = true
    }

    override fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(song).toMutableList()


    override fun getActionsRows(field: InfoActions.FieldType): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(song, field)

}