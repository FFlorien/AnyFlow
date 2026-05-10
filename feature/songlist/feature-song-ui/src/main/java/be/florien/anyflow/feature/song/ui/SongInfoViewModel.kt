package be.florien.anyflow.feature.song.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import be.florien.anyflow.common.navigation.Navigator
import be.florien.anyflow.common.ui.domain.ImageConfig
import be.florien.anyflow.feature.song.base.domain.BaseSongInfoActions.Companion.DUMMY_SONG_ID
import be.florien.anyflow.feature.song.base.domain.model.BaseSongInfoRow
import be.florien.anyflow.feature.song.base.domain.model.SongActionType
import be.florien.anyflow.feature.song.base.domain.model.SongFieldType
import be.florien.anyflow.feature.song.base.ui.BaseSongViewModel
import be.florien.anyflow.feature.song.domain.SongInfoActions
import be.florien.anyflow.management.download.DownloadManager
import be.florien.anyflow.management.filters.domain.model.FilterType
import be.florien.anyflow.tags.DataRepository
import be.florien.anyflow.urls.UrlRepository
import be.florien.anyflow.tags.model.SongInfo
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongInfoViewModel @Inject constructor(
     val infoActions: SongInfoActions,
    private val dataRepository: DataRepository,
    private val downloadManager: DownloadManager,
    private val urlRepository: UrlRepository,
    val navigator: Navigator
) : BaseSongViewModel() {

    override var songId: Long
        get() {
            val value = songInfoMediator.value?.id
            return value ?: SongInfo.dummySongInfo().id
        }
        set(value) {
            viewModelScope.launch {
                if (value != DUMMY_SONG_ID) {
                    songInfoMediator.addSource(dataRepository.getSong(value)) {
                        songInfoMediator.mutable.value = it

                        coverConfig.mutable.value = ImageConfig(
                            url = urlRepository.getAlbumArtUrl(it.albumId),
                            resource = R.drawable.cover_placeholder
                        )
                        updateRows()
                    }
                }
            }
        }

    val searchTerm: LiveData<String> = MutableLiveData(null)
    val isPlaylistListDisplayed: LiveData<PlaylistSelectionData> = MutableLiveData(null)

    override fun executeAction(row: BaseSongInfoRow): Boolean {
        val actionType = row.actionType
        val fieldType = row.fieldType
        if (super.executeAction(row)) {
            return true
        }

        viewModelScope.launch {
            when (actionType) {
                SongActionType.AddNext -> infoActions.playNext(songId)
                SongActionType.AddToPlaylist -> displayPlaylistList(
                    fieldType,
                    (row as? BaseSongInfoRow.SongMultipleInfoRow)?.index ?: 0
                )

                SongActionType.AddToFilter -> infoActions.filterOn(
                    songInfo,
                    row
                )

                SongActionType.Search ->
                    searchTerm.mutable.value = infoActions.getSearchTerms(songInfo, fieldType)

                SongActionType.Download -> {
                    val index = (row as? BaseSongInfoRow.SongMultipleInfoRow)?.index
                    infoActions.queueDownload(songInfo, fieldType, index)
                }

                else -> return@launch
            }
        }
        return true
    }

    private fun displayPlaylistList(fieldType: SongFieldType, order: Int) {
        val id = when (fieldType) {
            SongFieldType.Title -> songInfo.id
            SongFieldType.Artist -> songInfo.artistId
            SongFieldType.Album,
            SongFieldType.Disk -> songInfo.albumId

            SongFieldType.AlbumArtist -> songInfo.albumArtistId
            SongFieldType.Genre -> songInfo.genreIds[order]
            SongFieldType.Playlist -> songInfo.playlistIds[order]
            else -> return
        }
        val secondId =
            if (fieldType == SongFieldType.Disk) songInfo.disk else null
        isPlaylistListDisplayed.mutable.value = PlaylistSelectionData(id, fieldType, secondId)
    }

    override fun getDownloadState(
        id: Long,
        type: FilterType,
        additionalInfo: Int?
    ) = downloadManager.getDownloadState(id, type, additionalInfo)


    class PlaylistSelectionData(
        val id: Long,
        val type: SongFieldType,
        val secondId: Int? = null
    )
}