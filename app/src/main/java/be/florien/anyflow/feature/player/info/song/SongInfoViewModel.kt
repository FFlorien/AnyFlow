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
) : BaseSongViewModel(
    filtersManager,
    orderComposer,
    dataRepository,
    sharedPreferences,
    downloadManager
) {

    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<Boolean> = MutableLiveData(false)

    override fun executeAction(row: InfoActions.InfoRow): Boolean {
        val actionType = row.actionType
        val fieldType = row.fieldType
        if (fieldType !is SongInfoActions.SongFieldType || actionType !is SongInfoActions.SongActionType) {
            return false
        }
        if (super.executeAction(row)) {
            return true
        }

        viewModelScope.launch {
            when (actionType) {
                SongInfoActions.SongActionType.AddNext -> infoActions.playNext(song.id)
                SongInfoActions.SongActionType.AddToPlaylist -> displayPlaylistList()
                SongInfoActions.SongActionType.AddToFilter -> infoActions.filterOn(song, row)
                SongInfoActions.SongActionType.Search -> searchTerm.mutable.value =
                    infoActions.getSearchTerms(song, fieldType)
                SongInfoActions.SongActionType.Download -> {
                    when (fieldType) { //todo playlist when displayed in info!
                        SongInfoActions.SongFieldType.Title -> infoActions.download(song)
                        SongInfoActions.SongFieldType.Album -> infoActions.batchDownload(
                            song.albumId,
                            Filter.FilterType.ALBUM_IS
                        )
                        SongInfoActions.SongFieldType.AlbumArtist -> infoActions.batchDownload(
                            song.albumArtistId,
                            Filter.FilterType.ALBUM_ARTIST_IS
                        )
                        SongInfoActions.SongFieldType.Artist -> infoActions.batchDownload(
                            song.artistId,
                            Filter.FilterType.ARTIST_IS
                        )
                        SongInfoActions.SongFieldType.Genre -> {
                            val index  = (row as SongInfoActions.SongMultipleInfoRow).index
                            infoActions.batchDownload(
                                song.genreIds[index],
                                Filter.FilterType.GENRE_IS
                            )
                        }
                        else -> return@launch
                    }
                    updateRows()
                }
                else -> return@launch
            }
        }
        return true
    }

    private fun displayPlaylistList() {
        isPlaylistListDisplayed.mutable.value = true
    }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(song).toMutableList()


    override suspend fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(song, row)
}