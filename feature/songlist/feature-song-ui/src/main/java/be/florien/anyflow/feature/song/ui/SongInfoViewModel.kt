package be.florien.anyflow.feature.song.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.data.ImageConfig
import be.florien.anyflow.common.ui.data.info.InfoActions
import be.florien.anyflow.feature.song.base.ui.BaseSongInfoActions
import be.florien.anyflow.feature.song.base.ui.BaseSongViewModel
import be.florien.anyflow.feature.song.domain.SongInfoActions
import be.florien.anyflow.tags.DataRepository
import be.florien.anyflow.tags.model.SongInfo
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongInfoViewModel @Inject constructor(
    override val infoActions: SongInfoActions,
    private val dataRepository: DataRepository,
    val navigator: Navigator
) : BaseSongViewModel<SongInfoActions>() {

    override var songId: Long
        get() {
            val value = songInfoMediator.value?.id
            return value ?: SongInfo.dummySongInfo().id
        }
        set(value) {
            viewModelScope.launch {
                if (value != BaseSongInfoActions.DUMMY_SONG_ID) {
                    songInfoMediator.addSource(dataRepository.getSong(value)) {
                        songInfoMediator.mutable.value = it

                        coverConfig.mutable.value = ImageConfig(
                            url = infoActions.getAlbumArtUrl(it.albumId),
                            resource = R.drawable.cover_placeholder
                        )
                        updateRows()
                    }
                }
            }
        }

    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<PlaylistSelectionData> = MutableLiveData(null)

    override fun executeAction(row: InfoActions.InfoRow): Boolean {
        val actionType = row.actionType
        val fieldType = row.fieldType
        if (fieldType !is BaseSongInfoActions.SongFieldType || actionType !is BaseSongInfoActions.SongActionType) {
            return false
        }
        if (super.executeAction(row)) {
            return true
        }

        viewModelScope.launch {
            when (actionType) {
                BaseSongInfoActions.SongActionType.AddNext -> infoActions.playNext(songId)
                BaseSongInfoActions.SongActionType.AddToPlaylist -> displayPlaylistList(
                    fieldType,
                    (row as? BaseSongInfoActions.SongMultipleInfoRow)?.index ?: 0
                )

                BaseSongInfoActions.SongActionType.AddToFilter -> infoActions.filterOn(
                    songInfo,
                    row
                )

                BaseSongInfoActions.SongActionType.Search ->
                    searchTerm.mutable.value = infoActions.getSearchTerms(songInfo, fieldType)

                BaseSongInfoActions.SongActionType.Download -> {
                    val index = (row as? BaseSongInfoActions.SongMultipleInfoRow)?.index
                    infoActions.queueDownload(songInfo, fieldType, index)
                }

                else -> return@launch
            }
        }
        return true
    }

    private fun displayPlaylistList(fieldType: BaseSongInfoActions.SongFieldType, order: Int) {
        val id = when (fieldType) {
            BaseSongInfoActions.SongFieldType.Title -> songInfo.id
            BaseSongInfoActions.SongFieldType.Artist -> songInfo.artistId
            BaseSongInfoActions.SongFieldType.Album,
            BaseSongInfoActions.SongFieldType.Disk -> songInfo.albumId

            BaseSongInfoActions.SongFieldType.AlbumArtist -> songInfo.albumArtistId
            BaseSongInfoActions.SongFieldType.Genre -> songInfo.genreIds[order]
            BaseSongInfoActions.SongFieldType.Playlist -> songInfo.playlistIds[order]
            else -> return
        }
        val secondId =
            if (fieldType == BaseSongInfoActions.SongFieldType.Disk) songInfo.disk else null
        isPlaylistListDisplayed.mutable.value = PlaylistSelectionData(id, fieldType, secondId)
    }

    override suspend fun getInfoRowList(): MutableList<InfoActions.InfoRow> =
        infoActions.getInfoRows(songInfo).toMutableList()


    override suspend fun getActionsRowsFor(row: InfoActions.InfoRow): List<InfoActions.InfoRow> =
        infoActions.getActionsRows(songInfo, row)

    class PlaylistSelectionData(
        val id: Long,
        val type: BaseSongInfoActions.SongFieldType,
        val secondId: Int? = null
    )
}