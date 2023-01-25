package be.florien.anyflow.feature.player.info.song

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.data.view.Filter
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongInfoViewModel @Inject constructor(
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    sharedPreferences: SharedPreferences,
    downloadManager: DownloadManager
) : BaseSongViewModel(filtersManager, orderComposer, dataRepository, sharedPreferences, downloadManager) {

    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<Boolean> = MutableLiveData(false)

    override val infoActions = SongInfoActions(
        filtersManager,
        orderComposer,
        dataRepository,
        sharedPreferences,
        downloadManager
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
                    when (fieldType) { //todo playlist when displayed in info!
                        is InfoActions.SongFieldType.Title ->infoActions.download(song)
                        is InfoActions.SongFieldType.Album -> infoActions.batchDownload(song.albumId, Filter.FilterType.ALBUM_IS)
                        is InfoActions.SongFieldType.AlbumArtist -> infoActions.batchDownload(song.albumArtistId, Filter.FilterType.ALBUM_ARTIST_IS)
                        is InfoActions.SongFieldType.Artist -> infoActions.batchDownload(song.artistId, Filter.FilterType.ARTIST_IS)
                        is InfoActions.SongFieldType.Genre -> infoActions.batchDownload(song.genreIds.first(), Filter.FilterType.GENRE_IS) //todo :-(
                        else -> return@launch
                    }
                    updateRows()
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
        infoActions.getActionsRows(song, field)
}