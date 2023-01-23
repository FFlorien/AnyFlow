package be.florien.anyflow.feature.player.info.song

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.*
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongInfoViewModel @Inject constructor(
    context: Context,
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    val dataRepository: DataRepository,
    sharedPreferences: SharedPreferences
) : BaseSongViewModel(context, filtersManager, orderComposer, dataRepository, sharedPreferences) {
    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<Boolean> = MutableLiveData(false)
    private val downloadProgress: LiveData<Int> = MediatorLiveData(0)

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
                is InfoActions.SongActionType.Download -> {
                    val download = infoActions.download(song)
                    (downloadProgress as MediatorLiveData).addSource(download.asLiveData()) {
                        downloadProgress.mutable.value = it * 100 / song.size
                        if (downloadProgress.value == 100) {
                            viewModelScope.launch(Dispatchers.IO) {
                                dataRepository.getSong(song.id)?.let { updatedSong ->
                                    song = updatedSong
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun mapActionsRows(initialList: List<InfoActions.InfoRow>): List<InfoActions.InfoRow> =
        initialList

    private fun displayPlaylistList() {
        isPlaylistListDisplayed.mutable.value = true
    }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(song).toMutableList()


    override suspend fun getActionsRows(field: InfoActions.FieldType): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(song, field).toMutableList().apply {
            firstOrNull {
                it.fieldType is InfoActions.SongFieldType.Title
                        && it.actionType is InfoActions.SongActionType.Download
            }?.let {
                val index = indexOf(it)
                remove(it)
                val downloadWithProgress = it.copy(progress = downloadProgress)
                add(index, downloadWithProgress)
            }
        }
}