package be.florien.anyflow.feature.player.info.song

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.data.DataRepository
import be.florien.anyflow.data.DownloadManager
import be.florien.anyflow.data.UrlRepository
import be.florien.anyflow.feature.player.info.InfoActions
import be.florien.anyflow.player.FiltersManager
import be.florien.anyflow.player.OrderComposer
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongInfoViewModel @Inject constructor(
    filtersManager: FiltersManager,
    orderComposer: OrderComposer,
    dataRepository: DataRepository,
    urlRepository: UrlRepository,
    sharedPreferences: SharedPreferences,
    downloadManager: DownloadManager
) : BaseSongViewModel(
    filtersManager,
    orderComposer,
    dataRepository,
    urlRepository,
    sharedPreferences,
    downloadManager
) {

    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<PlaylistSelectionData> = MutableLiveData(null)

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
                SongInfoActions.SongActionType.AddNext -> infoActions.playNext(songId)
                SongInfoActions.SongActionType.AddToPlaylist -> displayPlaylistList(fieldType, (row as? SongInfoActions.SongMultipleInfoRow)?.index ?: 0)
                SongInfoActions.SongActionType.AddToFilter -> infoActions.filterOn(songInfo, row)
                SongInfoActions.SongActionType.Search ->
                    searchTerm.mutable.value = infoActions.getSearchTerms(songInfo, fieldType)
                SongInfoActions.SongActionType.Download -> {
                    val index = (row as? SongInfoActions.SongMultipleInfoRow)?.index
                    infoActions.queueDownload(songInfo, fieldType, index)
                }
                else -> return@launch
            }
        }
        return true
    }

    private fun displayPlaylistList(fieldType: SongInfoActions.SongFieldType, order: Int) {
        val id = when (fieldType) {
             SongInfoActions.SongFieldType.Title -> songInfo.id
            SongInfoActions.SongFieldType.Artist -> songInfo.artistId
            SongInfoActions.SongFieldType.Album -> songInfo.albumId
            SongInfoActions.SongFieldType.AlbumArtist -> songInfo.albumArtistId
            SongInfoActions.SongFieldType.Genre -> songInfo.genreIds[order]
            SongInfoActions.SongFieldType.Playlist -> songInfo.playlistIds[order]
            else -> return
        }
        isPlaylistListDisplayed.mutable.value = PlaylistSelectionData(id, fieldType)
    }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(songInfo).toMutableList()


    override suspend fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(songInfo, row)

    class PlaylistSelectionData(val id: Long, val type: SongInfoActions.SongFieldType)
}